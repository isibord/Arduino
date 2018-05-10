#include "ble_config.h"

/*
 * Red Bear Duo Interactive Nite Lite Arduino 
 * Created by Daisy Isibor 5/9/2018
 * 
 * The Library is created based on Bjorn's code for RedBear BLE communication: 
 * https://github.com/bjo3rn/idd-examples/tree/master/redbearduo/examples/ble_led
 * 
 * Our code is created based on the provided example code (Simple Controls) by the RedBear Team:
 * https://github.com/RedBearLab/Android
 * 
 * The code is also based off sample:
 * https://github.com/jonfroehlich/CSE590Sp2018/tree/master/L06-BLE/InClassExe2-RGBLed/RedBearDuoInClassEXE2
 */ 

#if defined(ARDUINO) 
SYSTEM_MODE(SEMI_AUTOMATIC); 
#endif

#define COMMON_ANODE 1

#define RECEIVE_MAX_LEN    3
#define BLE_SHORT_NAME_LEN 0x08 // must be in the range of [0x01, 0x09]
#define BLE_SHORT_NAME 'A','r','d','u','i','n','o'  // define each char but the number of char should be BLE_SHORT_NAME_LEN-1

/* Define the pins on the Duo board
 */
#define PWM_G_PIN                    D1
#define PWM_R_PIN                    D2
#define PWM_B_PIN                    D0

#define SLIDER_INPUT_PIN                A0
#define PHOTOCELL_INPUT_PIN             A1

// Set the min and max photocell values (this will be based on
// the brightness of your environment and the size of the voltage-divider
// resistor that you selected). I used 10kOhms 
const int MIN_PHOTOCELL_VAL = 25; // Photocell reading in dark
const int MAX_PHOTOCELL_VAL = 600; // Photocell reading in ambient light (tested in my apartment)

const int DELAY = 500; // delay between changing colors on slider
int priorSliderValue = 0;

int redValue = 255;
int greenValue = 255;
int blueValue = 255;

// UUID is used to find the device by other BLE-abled devices
static uint8_t service1_uuid[16]    = { 0x71,0x3d,0x00,0x00,0x50,0x3e,0x4c,0x75,0xba,0x94,0x31,0x48,0xf1,0x8d,0x94,0x1e };
static uint8_t service1_tx_uuid[16] = { 0x71,0x3d,0x00,0x03,0x50,0x3e,0x4c,0x75,0xba,0x94,0x31,0x48,0xf1,0x8d,0x94,0x1e };

// Define the configuration data
static uint8_t adv_data[] = {
  0x02,
  BLE_GAP_AD_TYPE_FLAGS,
  BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE, 
  
  BLE_SHORT_NAME_LEN,
  BLE_GAP_AD_TYPE_SHORT_LOCAL_NAME,
  BLE_SHORT_NAME, 
  
  0x11,
  BLE_GAP_AD_TYPE_128BIT_SERVICE_UUID_COMPLETE,
  0x1e,0x94,0x8d,0xf1,0x48,0x31,0x94,0xba,0x75,0x4c,0x3e,0x50,0x00,0x00,0x3d,0x71 
};

// Define the receive and send handlers
static uint16_t receive_handle = 0x0000;

static uint8_t receive_data[RECEIVE_MAX_LEN] = { 0x01 };

/**
 * @brief Callback for writing event.
 *
 * @param[in]  value_handle  
 * @param[in]  *buffer       The buffer pointer of writting data.
 * @param[in]  size          The length of writting data.   
 *
 * @retval 
 */
int bleWriteCallback(uint16_t value_handle, uint8_t *buffer, uint16_t size) {
  Serial.print("Write value handler: ");
  Serial.println(value_handle, HEX);

  if (receive_handle == value_handle) {
    memcpy(receive_data, buffer, RECEIVE_MAX_LEN);
    Serial.print("Write value: ");
    for (uint8_t index = 0; index < RECEIVE_MAX_LEN; index++) {
      Serial.print(receive_data[index], HEX);
      Serial.print(" ");
    }
    Serial.println(" ");
    
    /* Process the data
     * Receive the data sent from other BLE-abled devices (e.g., Android app)
     * and process the data for different purposes (digital write, digital read, analog read, PWM write)
     */
    if(receive_data[0] == 0x05) { // Command is to control PWM RED pin
      redValue = receive_data[1];
    }
    else if(receive_data[0] == 0x06) { // Command is to control PWM GREEN pin
      greenValue = receive_data[1];
    }
    else if(receive_data[0] == 0x07) { // Command is to control PWM BLUE pin
      blueValue = receive_data[1];
    }
    else if (receive_data[0] == 0x04) { // Command is to initialize all.
      analogWrite(PWM_R_PIN, 255);
      analogWrite(PWM_G_PIN, 255); 
      analogWrite(PWM_B_PIN, 255); 
    }
  }
  return 0;
}

void setup() {
  Serial.begin(115200);
  delay(DELAY);
  Serial.println("Starting Setup.");

  // Initialize ble_stack.
  ble.init();
  configureBLE(); //lots of standard initialization hidden in here - see ble_config.cpp
  // Set BLE advertising data
  ble.setAdvertisementData(sizeof(adv_data), adv_data);

  // Register BLE callback functions
  ble.onDataWriteCallback(bleWriteCallback);

  // Add user defined service and characteristics
  ble.addService(service1_uuid);
  receive_handle = ble.addCharacteristicDynamic(service1_tx_uuid, ATT_PROPERTY_NOTIFY|ATT_PROPERTY_WRITE|ATT_PROPERTY_WRITE_WITHOUT_RESPONSE, receive_data, RECEIVE_MAX_LEN);
  
  // BLE peripheral starts advertising now.
  ble.startAdvertising();
  Serial.println("BLE start advertising.");

  /*
   * Initialize all peripheral/pin modes
   */
  pinMode(PWM_G_PIN, OUTPUT);
  pinMode(PWM_R_PIN, OUTPUT); 
  pinMode(PWM_B_PIN, OUTPUT);
  analogWrite(PWM_G_PIN, 255);
  analogWrite(PWM_R_PIN, 255); 
  analogWrite(PWM_B_PIN, 255);
  
  pinMode(SLIDER_INPUT_PIN, INPUT);
  pinMode(PHOTOCELL_INPUT_PIN, INPUT);

  int sliderVal = analogRead(SLIDER_INPUT_PIN);
  priorSliderValue = map(sliderVal, 0, 4095, 0, 8);
} 

void loop() {
  int newSliderValue = analogRead(SLIDER_INPUT_PIN);
  int threeBitVal = map(newSliderValue, 0, 4095, 0, 8);

  // Read the photo-sensitive resistor value
  int photocellVal = analogRead(PHOTOCELL_INPUT_PIN);
  // Remap the value for output. 
  int ledBrightnessVal = map(photocellVal, MIN_PHOTOCELL_VAL, MAX_PHOTOCELL_VAL, 1, 10);

  ledBrightnessVal = constrain(ledBrightnessVal, 1, 10);
  
  if(priorSliderValue != threeBitVal){
    priorSliderValue = threeBitVal;
    mapBitValToColor(threeBitVal);
  }
  setColor(redValue, greenValue, blueValue, ledBrightnessVal);
 }

 void mapBitValToColor(int threeBitVal){
  //Here I'm using 3 bit representation
  //to determine which R, G, or B value is off (0) or on (1, in this case 255)
  
  redValue = ((threeBitVal >> 0) % 2) == 1 ? 255 : 0;
  greenValue = ((threeBitVal >> 1) % 2) == 1 ? 255 : 0;
  blueValue = ((threeBitVal >> 2) % 2) == 1 ? 255 : 0;

  //setColor(red, green, blue);
  Serial.println("SLIDED!");
  delay(DELAY);
}

void setColor(int red, int green, int blue, int brightness)
{ 
  red = red / brightness;
  green = green / brightness;
  blue = blue / brightness;
  
  #ifdef COMMON_ANODE
    red = 255 - red;
    green = 255 - green;
    blue = 255 - blue;
  #endif
  
  analogWrite(PWM_R_PIN, red);
  analogWrite(PWM_G_PIN, green);
  analogWrite(PWM_B_PIN, blue); 
  
}
