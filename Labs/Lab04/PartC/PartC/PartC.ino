/*
 Name:		PartC.ino
 Created:	4/14/2016 3:41:19 PM
 Author:	schackma
*/

#include <Servo.h> 
#include <ArmServos.h>
#include <RobotAsciiCom.h>


ArmServos robotArm;

RobotAsciiCom robotCom;



void setup(){
	robotArm.attach();

	Serial.begin(9600);  // Any speed is ok
						 // Register functions to be called when that command is received.
	robotCom.registerPositionCallback(positionCallback);
	robotCom.registerJointAngleCallback(jointAngleCallback);
	robotCom.registerGripperCallback(gripperCallback);

}

void loop() {
	if (Serial.available() > 0) {
		robotCom.handleRxByte(Serial.read());
	}
}


void serialEvent() {
	while (Serial.available()) {
		robotCom.handleRxByte(Serial.read());
	}
}


// Called when a POSITION command is received.
void positionCallback(int joint1Angle, int joint2Angle, int joint3Angle, int joint4Angle, int joint5Angle) {
	// Inform ArmServo of the position command.
	robotArm.setPosition(joint1Angle, joint2Angle, joint3Angle, joint4Angle, joint5Angle);

}

// Called when a JOINT ANGLE command is received.
void jointAngleCallback(byte jointNumber, int jointAngle) {
	// Inform ArmServo of the set joint angle command.
	robotArm.setJointAngle(jointNumber, jointAngle);
}

// Called when a GRIPPER command is received.
void gripperCallback(int gripperDistance) {
	// Inform ArmServo of the gripper command.
	robotArm.setGripperDistance(gripperDistance);
}

