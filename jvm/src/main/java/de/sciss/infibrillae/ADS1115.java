// Distributed with a free-will license.
// Use it any way you want, profit or free, provided it fits in the licenses of its associated works.
// ADS1115
// This code is designed to work with the ADS1115_I2CADC I2C Mini Module available from ControlEverything.com.
// https://www.controleverything.com/content/Analog-Digital-Converters?sku=ADS1115_I2CADC#tabs-0-product_tabset-2

package de.sciss.infibrillae;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

public class ADS1115
{
    public static void main(String[] args) throws Exception
    {
        // Create I2C bus
        I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
        // Get I2C device, ADS1115 I2C address is 0x48(72)
        I2CDevice device = bus.getDevice(0x48);

        // 0x84 = binary 1 000 010 0
        // bit 15: Operational Status. 1 = Start a single conversion.
        // bits 14-12: Input multiplexer configuration. 000 = default
        // bits 11-9: Programmable gain amplifier configuration. 000 = +- 6.144V, 001 = +- 4.096V,
        //    010 = +- 2.048V (default), 011 = +- 1.024V, 100 = +- 0.512V, 101 or 110 or 111 = +- 0.256V
        // bit 8: Device operating mode. 0 = continuous, 1 = single shot or power-down state
        //
        // 0x83 = binary 100 00011.
        // bits 7-5: Data rate. 000 = 8 Hz, 001 = 16 Hz, 010 = 32 Hz, 011 = 64 Hz, 100 = 128 Hz (default),
        //    101 = 250 Hz, 110 = 475 Hz, 111 = 860 Hz
        // bit 4: Comparator mode. 0 = traditional, 1 = windowed
        // bit 3: Comparator polarity of the ALERT/RDY pin. 0 = active low, 1 = active high
        // bit 2: Latching comparator. 0 = non-latching, 1 = latching
        // bits 1-0: Comparator queue and disable. 00 = Assert after one conversion, 01 = Assert after two conversions,
        //    10 = Assert after four conversions, 11 = Disable comparator (default)

        // AINP = AIN0 and AINN = AIN1, +/- 2.048V, Continuous conversion mode, 128 SPS
        byte[] config = { (byte) 0x84, (byte) 0x83 };
        // Select configuration register
        device.write(0x01, config, 0, 2);
        Thread.sleep(500);

        while (true) {
            // Read 2 bytes of data
            // raw_adc msb, raw_adc lsb
            byte[] data = new byte[2];
            device.read(0x00, data, 0, 2);

            // Convert the data
            int raw_adc = ((data[0] & 0xFF) * 256) + (data[1] & 0xFF);
            if (raw_adc > 32767)
            {
                raw_adc -= 65535;
            }

            // Output data to screen
            System.out.printf("AIN0: %d \n", raw_adc);
            Thread.sleep(250);
      }
    }
}
