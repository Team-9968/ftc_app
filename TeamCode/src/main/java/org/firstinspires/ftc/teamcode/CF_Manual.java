package org.firstinspires.ftc.teamcode;

import android.graphics.Path;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;

import java.util.concurrent.TimeUnit;

/**
 * Created by Ryley on 9/25/17.
 */
@TeleOp(name="CF_Manual", group = "code")
//@Disabled
public class CF_Manual extends OpMode {
    // Instance of Robot
    CF_Hardware robot = new CF_Hardware();
    CF_Manual_Motor_Library driveMan = new CF_Manual_Motor_Library();
    CF_Accessory_Motor_Library accessory = new CF_Accessory_Motor_Library();
    // Instantiates variables
    double position = 0.33;
    int mode = 0;

    double positionUpper = 0.51;
    double positionLower = 0.6;

    boolean changeDirectionLast = false;
    boolean changeDirection = false;

    int invert = 1;

    boolean lastY = false;
    boolean Y = false;

    boolean lastA = false;
    boolean A = false;

    boolean lastB = false;
    boolean B = false;

    boolean lastRB = false;
    boolean RB = false;

    boolean lastLB = false;
    boolean LB = false;

    boolean X = false;
    boolean lastX = false;

    boolean down = false;
    boolean lastDown = false;

    boolean up = false;
    boolean lastUp = false;

    double start = 0;
    double end = 0;

    double startTime = 0;

    enum mastDown {
        START, STOP, STANDBY
    }

    mastDown mDown = mastDown.STANDBY;

    public void init() {
        // Inits robot
        robot.init(hardwareMap);
        telemetry.addData("", "init");
        start = robot.clawMotor.getCurrentPosition();
//        robot.jewelHitter.setPosition(0.333);
//        robot.colorArm.setPosition(1.0);

        end = start + 1719;
        robot.leftFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        robot.rightFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        robot.leftRear.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        robot.rightRear.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Inits masts to down position
//        while(robot.limit.getState()) {
//            accessory.setPowerToPower(robot.clawMotor, 1, 3);
//            accessory.setPowerToPower(robot.mastMotor, 1, 3);
//        }
//        accessory.setPowerToPower(robot.clawMotor, 0, 3);
//        accessory.setPowerToPower(robot.mastMotor, 0, 3);
    }

    public void loop(){
        // Calls appropriate methods to run the robot.  These 3 methods do everything that the robot does, excepting telemetry
        //updateMode();
        drive();
        lift();
        clamp();

        telemetry.clearAll();
        telemetry.addData("Mode", mode);
        telemetry.addData("Position Side", robot.colorArm.getPosition());
        telemetry.addData("Position Upper", positionUpper);
        telemetry.addData("Position Lower", positionLower);
        telemetry.addData("Position Claw", robot.clawMotor.getCurrentPosition());
        telemetry.update();

    }

    // Updates drive mode.  0 = normal mech, 1 = tank, 2 = slow mech, 3 = backwards mech
    public void updateMode() {
        if(gamepad1.a) {
            if(mode == 3) {
                mode = 0;
            }
            else if(mode < 3) {
                mode++;
            }

            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (Exception e) {}
        }
    }
    // Implements the drive modes
    public void drive() {
        // Implements Owen's switchy thingamajigger
        changeDirection = gamepad1.y;
        if(!changeDirectionLast && changeDirection) {
            //Invert is the multiplyer to switch the gizmo
            invert = -1 * invert;
        }
        // Mode to drive mechanum wheels forward at 100 percent power
        if (mode == 0) {
            driveMan.changeDirectonAndPower(1);
            driveMan.runMechWheels(robot, invert * gamepad1.left_stick_y, invert * gamepad1.left_stick_x, gamepad1.right_stick_x, 3);
        }
        // Mode for tank mode
        if (mode == 1) {
            driveMan.tankMode(robot, invert * gamepad1.left_stick_y, invert * gamepad1.right_stick_y);
        }
        // Mode for half power forward mechanum
        if (mode == 2) {
            driveMan.changeDirectonAndPower(0.5);
            driveMan.runMechWheels(robot, invert * gamepad1.left_stick_y, invert * gamepad1.left_stick_x, gamepad1.right_stick_x, 3);
        }
        // Mode for full power backwards mechanum
        if (mode == 3) {
            driveMan.changeDirectonAndPower(-1);
            driveMan.runMechWheels(robot, invert * gamepad1.left_stick_y, invert * gamepad1.left_stick_x, gamepad1.right_stick_x, 3);
        }
        changeDirectionLast = changeDirection;
    }

    // Implements the lifter motors
    public void lift() {
        //To get rid of the quick lift method, comment out everything in this method except for the top two lines.
        accessory.setPowerToPower(robot.clawMotor, gamepad2.right_stick_y, 3);
        accessory.setPowerToPower(robot.mastMotor, gamepad2.left_stick_y, 3);
        down = gamepad2.dpad_down;

        switch(mDown) {
            case STANDBY:
                if (down && !lastDown) {
                    mDown = mastDown.START;
                    startTime = getRuntime();
                }
                break;
            case START:
                accessory.setPowerToPower(robot.clawMotor, 1, 3);
                accessory.setPowerToPower(robot.mastMotor, 1, 3);
                // Implemented 5 second timeout
                if (!robot.limit.getState() || getRuntime() - startTime > 5000) {
                    mDown = mastDown.STOP;
                }
                break;
            case STOP:
                accessory.setPowerToPower(robot.clawMotor, 0, 3);
                accessory.setPowerToPower(robot.mastMotor, 0, 3);
                positionLower = 0.6;
                positionUpper = 0.51;
                mDown = mastDown.STANDBY;
                break;
        }
    }

    // Clamps the block
    public void clamp() {
        A = gamepad2.a;
        Y = gamepad2.y;
        RB = gamepad2.right_bumper;
        LB = gamepad2.left_bumper;
        B = gamepad2.b;
        X = gamepad2.x;
        up = gamepad2.dpad_up;

        if(X) {
            position += 0.001;
        }
        if(B) {
            position -= 0.001;
        }

        if(!lastA && A) {
            //0.3
            if(positionLower == 0.6) {
                positionLower = 0.3;
            } else if(positionLower == 0.3) {
                positionLower = 0.6;
            } else if(positionLower == 0.46) {
                positionLower = 0.6;
            }
        }

        // 0.81 0.41
        // Debouncing for the buttons
        if(!lastY && Y) {
            if(positionUpper == 0.51) {
                positionUpper = 0.81;
            } else if(positionUpper == 0.81) {
                positionUpper = 0.51;
            } else if(positionUpper == 0.64) {
                positionUpper = 0.51;
            }
        }

        if(!lastRB && RB) {
            positionUpper = 0.51;
            positionLower = 0.6;
        }

        if(!lastLB && LB) {
            positionUpper = 0.81;
            positionLower = 0.3;
        }

        if(!lastUp && up) {
            positionUpper = 0.64;
            positionLower = 0.46;
        }

        robot.clamp.setPosition(positionUpper);
        robot.lowerClamp.setPosition(positionLower);

        //lower = 0.386
        //upper = 0.71  0.51
        lastX = X;
        lastB = B;
        lastY = Y;
        lastA = A;
        lastRB = RB;
        lastLB = LB;
        lastUp = up;
    }
}
