/*
 * Using Potentiometer value to change RGB LED color
 * 
 * By Daisy Isibor
 * 
 */

/* 
 * IMPORTANT: When working with the RedBear Duo, you must have this line of
 * code at the top of your program. The default state is SYSTEM_MODE(AUTOMATIC);
 * however, this puts the RedBear Duo in a special cloud-based mode that we 
 * are not using. For our purposes, set SYSTEM_MODE(SEMI_AUTOMATIC) or
 * SYSTEM_MODE(MANUAL). See https://docs.particle.io/reference/firmware/photon/#system-modes
 */
SYSTEM_MODE(MANUAL); 

#define COMMON_ANODE 1

const int POT_INPUT_PIN = A0;

const int RGB_RED_PIN = D0;
const int RGB_GREEN_PIN  = D1;
const int RGB_BLUE_PIN  = D2;
const int DELAY = 500; // delay between changing colors

void setup() {
  pinMode(POT_INPUT_PIN, INPUT);
  
  pinMode(RGB_RED_PIN, OUTPUT);
  pinMode(RGB_GREEN_PIN, OUTPUT);
  pinMode(RGB_BLUE_PIN, OUTPUT);

  // Turn on Serial so we can verify expected colors via Serial Monitor
  Serial.begin(9600); 
}

void loop() {

  int potVal = analogRead(POT_INPUT_PIN);
  mapPotToColor(potVal);
}

void setColor(int red, int green, int blue)
{
  #ifdef COMMON_ANODE
    red = 255 - red;
    green = 255 - green;
    blue = 255 - blue;
  #endif
  analogWrite(RGB_RED_PIN, red);
  analogWrite(RGB_GREEN_PIN, green);
  analogWrite(RGB_BLUE_PIN, blue);  
}

void mapPotToColor(int potVal){
  // the analogRead on the RedBear Duo seems to go from 0 to 4092 (and not 4095
  // as you would expect with a power of two--e.g., 2^12 or 12 bits). On the Arduino
  // Uno, the analogRead ranges from 0 to 1023 (2^10 or 10 bits). Regardless,
  // we need to remap this value linearly from the large range (0-4092) to
  // the smaller range (0-255) since the analogWrite function can only write out
  // 0-255 (a byte--2^8).  
  int threeBitVal = map(potVal, 0, 4092, 0, 7);

  //Here I'm using 3 bit representation from 0 to 7 
  //to determine which R, G, or B value is off (0) or on (1, in this case 255)
  
  int red = ((threeBitVal >> 0) % 2) == 1 ? 255 : 0;
  int green = ((threeBitVal >> 1) % 2) == 1 ? 255 : 0;
  int blue = ((threeBitVal >> 2) % 2) == 1 ? 255 : 0;

  setColor(red, green, blue);
  delay(DELAY);
}

