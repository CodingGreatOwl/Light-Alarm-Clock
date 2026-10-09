use esp32_nimble::{uuid128, BLEAdvertisementData, BLEDevice, NimbleProperties};
use esp_idf_svc::hal::ledc::*;
use esp_idf_svc::hal::peripherals::Peripherals;
use esp_idf_svc::hal::prelude::*;
use std::num::NonZeroU32;
use esp_idf_svc::hal::task::notification::Notification;
use esp_idf_svc::hal::timer::TimerDriver;
use esp_idf_svc::hal::timer::config::Config;
use esp_idf_svc::hal::sys::EspError;

fn main() -> Result<(), EspError> {

    enum TransmissionState {
        Stop,
        Start,
        PauseLead,
        StartBit,
        EndBit,
        StopBurst,
    }

    const TimerLeadPulse:u64 = 3600;
    const TimerLeadPause:u64 = 1800;
    const TimerStartBit:u64 = 225; // 1 tick = 2.5 us
    const TimerPauseTransmission:u64 = 45000;
    const TimerEndBit0:u64 = 225;
    const TimerEndBit1:u64 = 675;
    const TimerEndTransmission:u64 = 40000;
    const TimerStopBurst:u64 = 225;

    let mut current_state = TransmissionState::Stop;

    esp_idf_svc::hal::sys::link_patches();

    let peripherals = Peripherals::take()?;

    // Configure the LED PWM
    let mut channel = LedcDriver::new(
        peripherals.ledc.channel0,
        LedcTimerDriver::new(
            peripherals.ledc.timer0,
            &config::TimerConfig::new().frequency(38200.Hz().into()),
        )?,
        peripherals.pins.gpio22, // gpio25
    )?;
    channel.set_duty(channel.get_max_duty() / 2)?;
    channel.disable()?;

    // Initialize timer for interupts
    let config = Config::new().auto_reload(true).divider(65000); // 200 // 65000 to make it debugable
    let mut timer = TimerDriver::new(peripherals.timer10, &config).unwrap();

    let tick_per_second = timer.tick_hz();
    timer.set_alarm(tick_per_second * 2)?;
    println!("Ticks per seconds : {tick_per_second}");

    let notification = Notification::new();
    let notifier = notification.notifier();

    let ble_notification = Notification::new();
    let ble_update = ble_notification.notifier();

    // Register the function to be called when timer interrupt is triggered.
    unsafe {
        timer.subscribe(move || {
            let bitset = 0b10001010101;
            notifier.notify_and_yield(NonZeroU32::new(bitset).unwrap());
        })?;
    }


    let mut is_next_bit_zero = true;
    let mut bits_to_transmit = 32;
    let mut finish = false;

    let command = 0xF7C03F;

    let mut value_changed:bool = false;
    let mut transmission_started = false;
    let device_name = "ESP32 Perif";
    esp_idf_svc::sys::link_patches();

    // Take ownership of device
    let ble_device = BLEDevice::take();

    // Obtain handle for peripheral advertiser
    let ble_advertiser = ble_device.get_advertising();

    // Obtain handle for server
    let server = ble_device.get_server();

    // Define server connect behaviour
    server.on_connect(|server, clntdesc| {
        // Print connected client data
        println!("{:?}", clntdesc);
        // Update connection parameters
        server
            .update_conn_params(clntdesc.conn_handle(), 24, 48, 0, 60)
            .unwrap();
    });

    // Define server disconnect behaviour
    server.on_disconnect(|_desc, _reason| {
        println!("Disconnected, back to advertising");
    });

    // Create a service with custom UUID
    let remoteControlLed = server.create_service(uuid128!("6827989e-079a-404f-946a-da39f968ce82"));

    // Create a characteristic to associate with created service
    let startSequence = remoteControlLed.lock().create_characteristic(
        uuid128!("c696a5a1-827f-4df2-8260-5cdc7f0a4f44"),
        NimbleProperties::WRITE | NimbleProperties::READ );

    // Modify characteristic value
    startSequence.lock().set_value(b"Start Value");

    startSequence.lock().on_write(move |write_args| {
        println!("Received: {}", String::from_utf8_lossy(write_args.recv_data()));
        unsafe {
        ble_update.notify_and_yield(NonZeroU32::new(0b1).unwrap());
        }
    });

    // Configure Advertiser Data
    ble_advertiser
        .lock()
        .set_data(
            BLEAdvertisementData::new()
                .name(device_name)
                .add_service_uuid(uuid128!("6827989e-079a-404f-946a-da39f968ce82")),
        )
        .unwrap();

    // Start Advertising
    ble_advertiser.lock().start().unwrap();

    // (Optional) Print dump of local GATT table
    // server.ble_gatts_show_local();


    // Enable the timer interruption
    timer.enable_interrupt()?;
    timer.enable_alarm(true)?;
    timer.enable(true)?;

    let mut command_repetion = 0;

    loop {

        if value_changed
        {
            timer.enable(true)?;
            transmission_started = true;
            value_changed = false;
            command_repetion = 3;
        }
        else if transmission_started {
            let bitset = notification.wait(esp_idf_svc::hal::delay::BLOCK);

            if let Some(bitset) = bitset {
                match current_state {
                    TransmissionState::Stop => {
                        current_state = TransmissionState::Start;
                        channel.enable()?;
                        timer.set_alarm(TimerLeadPulse)?;
                        bits_to_transmit = 32;
                        println!("Reached first interrupt");
                    },
                    TransmissionState::Start => {
                        current_state = TransmissionState::PauseLead;
                        channel.disable()?;
                        timer.set_alarm(TimerLeadPause)?;
                        println!("Reached second interrupt");
                    }
                    TransmissionState::PauseLead => {
                        current_state = TransmissionState::StartBit;
                        channel.enable()?;
                        timer.set_alarm(TimerStartBit)?;

                        is_next_bit_zero = ((1 << (bits_to_transmit - 1) ) & command) == 0;
                        println!("Reached third interrupt");
                    }
                    TransmissionState::StartBit => {
                        channel.disable()?;
                        if is_next_bit_zero {
                            timer.set_alarm(TimerEndBit0)?;
                        }
                        else {
                            timer.set_alarm(TimerEndBit1)?;
                        }
                        current_state = TransmissionState::EndBit;

                        bits_to_transmit = bits_to_transmit - 1;
                        finish = bits_to_transmit == 0;

                        if is_next_bit_zero {
                            println!("Transmit 0");
                        }
                        else {
                            println!("Transmit 1");
                        }
                    }
                    TransmissionState::EndBit => {
                        channel.enable()?;
                        if finish {
                            current_state = TransmissionState::StopBurst;
                            timer.set_alarm(TimerStopBurst)?;
                        }
                        else {
                            timer.set_alarm(TimerStartBit)?;
                            current_state = TransmissionState::StartBit;
                            is_next_bit_zero = ((1 << (bits_to_transmit - 1) ) & command) == 0;
                        }
                    }
                    TransmissionState::StopBurst => {
                        channel.disable()?;
                        current_state = TransmissionState::Stop;
                        if command_repetion > 0 {
                            command_repetion -= 1;
                            timer.set_alarm(TimerPauseTransmission)?;
                            println!("Waiting to repeat");
                        }
                        else {
                            timer.enable(false)?;
                            println!("Reached end transmission");
                            transmission_started = false;
                        }
                    }
                }
            }

        }
        else {
            println!("Let's wait for someone triggering the ble");
            let bitset = ble_notification.wait(esp_idf_svc::hal::delay::BLOCK);
            // Why does it triggers at the start ?
            println!("All right, let's start the transmission");
            value_changed = true;
        }

    }
}
