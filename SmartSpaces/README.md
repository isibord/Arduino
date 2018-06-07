# SmartSpaces

[![SmartSpaces Demo](https://github.com/isibord/Arduino/blob/master/Pictures/YoutubeScreenShot-SmartSpaces.PNG)](https://www.youtube.com/watch?v=zhMIhOvSo_Y "SmartSpaces Demo")

Arduino, circuit, sensors+actuators: Designed the smart space device using a servo motor mounted on top of the box and it responds in real time to the location of the user as detected by the camera from the phone. An ultrasonic sensor is mounted on top of the servo, enabling it rotate towards the user. The ultrasonic sensor detects the distance to the detected user and sends it to Android for consumption.

![Fig1](https://github.com/isibord/Arduino/blob/master/Pictures/smartspace-circuit.jpg)

Android Smartphone app: The Android-based smartphone app detects faces in real time, tracks their location on the screen, sends the location back to Arduino so that the servo can rotate towards it. And it receives the distance measurement from Arduino and displays it on the top right corner of the screen.
	
![Fig2](https://github.com/isibord/Arduino/blob/master/Pictures/userinteracting-SmartSpaces.PNG)

Lo-fi enclosure: A Lo-fi enclosure made with a cardboard box, holds the servo and ultrasonic sensor on top, with all the circuitry hidden inside the box. Lastly, the android phone facing forward is glued to the front with camera towards the user.

![Fig3](https://github.com/isibord/Arduino/blob/master/Pictures/smartspace-setup.jpg)

3D enclosure: I later 3D-printed a stand for the servo motor to sit on a flat surface and a motor attachment for the ultrasonic sensor to sit on the servo propeller. 

![Fig4](https://github.com/isibord/Arduino/blob/master/Pictures/fabrication-3dprint.jpg)


![Fig5](https://github.com/isibord/Arduino/blob/master/Pictures/fabrication-setup.jpg)

Creative Component 1: My creative component is an accessibility feature that vocally describes the face that has been detected. It will say something like: “ I see a female of about age 31, wearing reading glasses and about 200 centimeters away”.

Creative Component 2: My second creative component (I call the snapchat effect :) ) used the android smiling probability + graphics overlay + face landmarks to show the detected state of the user's emotion via a smiley with 2 emotional states happy and sad.
![Fig6](https://github.com/isibord/Arduino/blob/master/Pictures/fabrication-emotion.png)

Update Demo with Fabrication:

[![SmartSpaces Demo](https://github.com/isibord/Arduino/blob/master/Pictures/fabrication-screenshot.jpg)](https://www.youtube.com/watch?v=xWQ1CzxLh7s&feature=youtu.be "SmartSpaces with Fabrication Demo")

Addendum: Microsoft cognitive service API (https://westus.dev.cognitive.microsoft.com/docs/services/563879b61984550e40cbbe8d/operations/563879b61984550f30395236) requires a subscription key. You need to update <Subscription Key> in code and the region in url as well.