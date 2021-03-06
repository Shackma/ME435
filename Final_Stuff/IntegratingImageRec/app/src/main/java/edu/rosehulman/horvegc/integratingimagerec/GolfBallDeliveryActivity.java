package edu.rosehulman.horvegc.integratingimagerec;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import me435.NavUtils;

public class GolfBallDeliveryActivity extends ImageRecActivity {

	/** Constant used with logging that you'll see later. */
	public static final String TAG = "GolfBallDelivery";


    /**
     * An enum used for variables when a ball color needs to be referenced.
     */
    public enum BallColor {
        NONE, BLUE, RED, YELLOW, GREEN, BLACK, WHITE
    }

    /**
     * An enum used for the mState variable that tracks the robot's current state.
     */
    public enum State {
        READY_FOR_MISSION,
        CALIBRATE_BALL_COLORS,
        CALIBRATE_STRAIGHT_DRIVING,
        GO_TO_NEAR_BALL_WITH_GPS,
        GO_TO_NEAR_BALL_WITH_IMAGE,
        DROP_NEAR_BALL,
        GO_TO_FAR_BALL_WITH_GPS,
        GO_TO_FAR_BALL_WITH_IMAGE,
        DROP_FAR_BALL,
        DROP_MID_BALL,
        DRIVE_TOWARDS_HOME_GPS,
        DRIVE_TOWARDS_HOME_IMAGE,
        WAIT_FOR_PICKUP,
        FIND_HEADING,
        GIVE_UP1,
        GIVE_UP2,
        GIVE_UP3,
        GIVE_UP4
    }

    /**
     * Tracks the robot's current state.
     */
    public State mState= State.READY_FOR_MISSION;
    private State prevState;

    /**
     * Instance of a helper method class that implements various script driving functions.
     */
    private Scripts mScripts;



    /**
     * An array (of size 3) that stores what color is present in each golf ball stand location.
     */
    public BallColor[] mLocationColors = new BallColor[]{BallColor.NONE, BallColor.NONE, BallColor.NONE};

    /**
     * Simple boolean that is updated when the Team button is pressed to switch teams.
     */
    public boolean mOnRedTeam = false;


    // ---------------------- UI References ----------------------
    /**
     * An array (of size 3) that keeps a reference to the 3 balls displayed on the UI.
     */
    private ImageButton[] mBallImageButtons;

    /**
     * References to the buttons on the UI that can change color.
     */
    private Button mTeamChangeButton, mGoOrMissionCompleteButton,mJumboButton;

    /**
     * An array constants (of size 7) that keeps a reference to the different ball color images resources.
     */
    // Note, the order is important and must be the same throughout the app.
    private static final int[] BALL_DRAWABLE_RESOURCES = new int[]{R.drawable.none_ball, R.drawable.blue_ball,
            R.drawable.red_ball, R.drawable.yellow_ball, R.drawable.green_ball, R.drawable.black_ball, R.drawable.white_ball};

    /**
     * TextViews that can change values.
     */
    private TextView mCurrentStateTextView, mStateTimeTextView, mGpsInfoTextView, mSensorOrientationTextView,
            mGuessXYTextView, mLeftDutyCycleTextView, mRightDutyCycleTextView, mMatchTimeTextView;

    private TextView mJumbotronXView, mJumbotronYView;

    private TextView dCurrentStateTextView, dStateTimeTextView, dGpsInfoTextView, dSensorOrientationTextView,
        dGuessXYTextView, dHeadingTime;


    private LinearLayout mJumboLayout;
    
    // ---------------------- End of UI References ----------------------

	
	// ---------------------- Mission strategy values ----------------------
    /** Constants for the known locations. */
    public static final double NEAR_BALL_GPS_X = 90;
    public static final double FAR_BALL_GPS_X = 240;

    private static final double FIND_CONE_TIME = 20000;


    /** Variables that will be either 50 or -50 depending on the balls we get. */
    private double mNearBallGpsY, mFarBallGpsY;
    public static final int RIGHT = -50;
    public static final int LEFT = 50;

    private double mCurrX, mCurrY, mCurrHeading;

    private static final double DROP_PERCENT = .12; //TODO: CHANGE TO A GOOD NUMBER

    /**
     * If that ball is present the values will be 1, 2, or 3.
     * If not present the value will be 0.
     * For example if we have the black ball, then mWhiteBallLocation will equal 0.
     */
    public int mNearBallLocation, mFarBallLocation, mWhiteBallLocation,triesForCone = 0;
    // ----------------- End of mission strategy values ----------------------
	
	
    // ---------------------------- Timing area ------------------------------
	/**
     * Time when the state began (saved as the number of millisecond since epoch).
     */
    private long mStateStartTime;

    /**
     * Time when the match began, ie when Go! was pressed (saved as the number of millisecond since epoch).
     */
    private long mMatchStartTime;

    private long mLastHeadingTime;

    /**
     * Constant that holds the maximum length of the match (saved in milliseconds).
     */
    private long MATCH_LENGTH_MS = 300000; // 5 minutes in milliseconds (5 * 60 * 1000)

    private long LOST_HEADING_THRESHOLD = 20000;

    private long PICKUP_THRESHOLD = 10000;

    /**
     * Method that is called 10 times per second for updates. Note, the setup was done within RobotActivity.
     */
    public void loop() {
        super.loop(); // Important to call super first so that the RobotActivity loop function is run first.
        // RobotActivity updated the mGuessX and mGuessY already. Here we need to display it.
        mStateTimeTextView.setText("" + getStateTimeMs() / 1000);
        mGuessXYTextView.setText("(" + (int) mGuessX + ", " + (int) mGuessY + ")");
//        mJumbotronXView.setText(""+(int) mCurrentGpsX);
//        mJumbotronYView.setText(""+(int)mCurrentGpsY);
        dHeadingTime.setText(""+getLastHeadingTimeMs()/1000);

        // Match timer
        long matchTimeMs = MATCH_LENGTH_MS;
        long timeRemainingSeconds = MATCH_LENGTH_MS / 1000;
        if (mState != State.READY_FOR_MISSION && mState != State.CALIBRATE_BALL_COLORS) {
            matchTimeMs = getMatchTimeMs();
            timeRemainingSeconds = (MATCH_LENGTH_MS - matchTimeMs) / 1000;
            if (getMatchTimeMs() > MATCH_LENGTH_MS) {
//                setState(State.READY_FOR_MISSION);
            }
        }
        mMatchTimeTextView.setText(getString(R.string.time_format, timeRemainingSeconds / 60, timeRemainingSeconds % 60));

        switch(mState){
            case READY_FOR_MISSION:
                break;
            case CALIBRATE_BALL_COLORS:
                break;
            case CALIBRATE_STRAIGHT_DRIVING:
                //handled in set state
                break;
            case GO_TO_NEAR_BALL_WITH_GPS:
                complexMove(NEAR_BALL_GPS_X,mNearBallGpsY, State.GO_TO_NEAR_BALL_WITH_IMAGE,State.DROP_NEAR_BALL);
                if(mConeFound ){ //&& NavUtils.getDistance(mGuessX,mGuessY,NEAR_BALL_GPS_X,mNearBallGpsY) < ACCEPTED_DISTANCE_AWAY_FT
                    setState(State.GO_TO_NEAR_BALL_WITH_IMAGE);
                }
                break;
            case GO_TO_NEAR_BALL_WITH_IMAGE:
                coneVisionLogic(State.DROP_NEAR_BALL);
                if(NavUtils.getDistance(mGuessX,mGuessY,NEAR_BALL_GPS_X,mNearBallGpsY) > ACCEPTED_DISTANCE_AWAY_FT){
                    Log.d(TAG,"going back to gps");
                    setState(State.GO_TO_NEAR_BALL_WITH_GPS);
                }
                break;
            case DROP_NEAR_BALL:
                //handled in setState
                sendWheelSpeed(0,0);
                triesForCone=0; //reset so far ball has 3 tries
                break;
            case GO_TO_FAR_BALL_WITH_GPS:
                complexMove(FAR_BALL_GPS_X,mFarBallGpsY, State.GO_TO_FAR_BALL_WITH_IMAGE,State.DROP_MID_BALL);
                if(mConeFound ){ //&& NavUtils.getDistance(mGuessX,mGuessY,FAR_BALL_GPS_X,mFarBallGpsY) < SWITCH_BACK_DISTANCE
                    setState(State.GO_TO_FAR_BALL_WITH_IMAGE);
                }
                break;
            case GO_TO_FAR_BALL_WITH_IMAGE:
                coneVisionLogic(State.DROP_MID_BALL);
                if(NavUtils.getDistance(mGuessX,mGuessY,FAR_BALL_GPS_X,mFarBallGpsY) > SWITCH_BACK_DISTANCE){
                    Log.d(TAG,"going back to gps");
                    setState(State.GO_TO_FAR_BALL_WITH_GPS);
                }
                break;
            case DROP_FAR_BALL:
                //handled in set state
                sendWheelSpeed(0,0);
                triesForCone=0; //reset so go home doesn't break
                break;
            case DRIVE_TOWARDS_HOME_GPS:
                complexMove(0,0, State.DRIVE_TOWARDS_HOME_IMAGE,null);
                break;
            case DRIVE_TOWARDS_HOME_IMAGE:
                coneVisionLogic(State.WAIT_FOR_PICKUP);
            case WAIT_FOR_PICKUP:
                if(getStateTimeMs() > PICKUP_THRESHOLD){
                    setState(State.DRIVE_TOWARDS_HOME_GPS);
                }
                break;
            case FIND_HEADING:
                if(getLastHeadingTimeMs() < LOST_HEADING_THRESHOLD){
                    setState(prevState);
                }
                break;
        }

    }
	// ----------------------- End of timing area --------------------------------
	
	
    // ---------------------------- Driving area ---------------------------------


    public void complexMove(double xGoal,double yGoal,State newState,State dropAlready){
        if(getLastHeadingTimeMs() > LOST_HEADING_THRESHOLD){
            setState(State.FIND_HEADING);
        }else if(NavUtils.getDistance(mGuessX,mGuessY,xGoal,yGoal) < ACCEPTED_DISTANCE_AWAY_FT){
            triesForCone++;
            if(triesForCone > 3){
                setState(dropAlready);
            }
            setState(newState);

        }else if(NavUtils.targetIsOnLeft(mGuessX,mGuessY,mCurrHeading,xGoal,yGoal)){
            double turnHeading = NavUtils.getLeftTurnHeadingDelta(mCurrHeading, NavUtils.getTargetHeading
                    (mGuessX,mGuessY,xGoal,yGoal));
            Log.d(TAG,"the robot is turning left at the heading: "+turnHeading);
            sendWheelSpeed((int)(mLeftStraightPwmValue - turnHeading*PCTRL),
                    mRightStraightPwmValue);

        }else{
            double turnHeading = NavUtils.getRightTurnHeadingDelta(mCurrHeading, NavUtils.getTargetHeading
                    (mGuessX,mGuessY,xGoal,yGoal));
            Log.d(TAG,"the robot is turning right at the heading: "+turnHeading);
            sendWheelSpeed(mLeftStraightPwmValue,
                    (int)(mRightStraightPwmValue-turnHeading*PCTRL));
        }
    }

    public void coneVisionLogic(State dropAlready) {
        if(!mConeFound){
            sendWheelSpeed((int).5*mLeftStraightPwmValue,mRightStraightPwmValue);
            if(getStateTimeMs() > FIND_CONE_TIME){
                setState(dropAlready);
            }
            return;
        }


        if(mConeSize > DROP_PERCENT) {
            setState(dropAlready); //you win!!!
        } else if (mConeLeftRightLocation > 0.2) { //You're leaning too far to the left., turn right.
            sendWheelSpeed(mLeftStraightPwmValue, mRightStraightPwmValue-40);
        } else if (mConeLeftRightLocation < -0.2) { //You're leaning too far to the right, turn left.
            sendWheelSpeed(mLeftStraightPwmValue-40, mRightStraightPwmValue);
        }else{
            sendWheelSpeed(mLeftStraightPwmValue,mRightStraightPwmValue);
        }

    }

    /**
     * Send the wheel speeds to the robot and updates the TextViews.
     */
    @Override
    public void sendWheelSpeed(int leftDutyCycle, int rightDutyCycle) {
        mJumbotronXView.setText("" + rightDutyCycle+", " + leftDutyCycle);
        super.sendWheelSpeed(leftDutyCycle, rightDutyCycle); // Send the values to the
        mLeftDutyCycleTextView.setText("Left\n" + leftDutyCycle);
        mRightDutyCycleTextView.setText("Right\n" + rightDutyCycle);
    }

    /**
     * When driving towards a target, using a seek strategy, consider that state a success when the
     * GPS distance to the target is less than (or equal to) this value.
     */
    public static final double ACCEPTED_DISTANCE_AWAY_FT = 10.0; //30.0;
    public static final double SWITCH_BACK_DISTANCE = 20.0; //40;
	
	/**
     * Multiplier used during seeking to calculate a PWM value based on the turn amount needed.
     */
    private static final double SEEKING_DUTY_CYCLE_PER_ANGLE_OFF_MULTIPLIER = 3.0;  // units are (PWM value)/degrees

    /**
     * Variable used to cap the slowest PWM duty cycle used while seeking. Pick a value from -255 to 255.
    */
    private static final int LOWEST_DESIRABLE_SEEKING_DUTY_CYCLE = 150;

    /**
     * PWM duty cycle values used with the drive straight dialog that make your robot drive straightest.
     */
//    public int mLeftStraightPwmValue = 215, mRightStraightPwmValue = 250;

    public int mLeftStraightPwmValue = 250, mRightStraightPwmValue = 250;

    private static final double PCTRL = .8;
	// ------------------------ End of Driving area ------------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_golf_ball_delivery);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mBallImageButtons = new ImageButton[]{(ImageButton) findViewById(R.id.location_1_image_button),
                (ImageButton) findViewById(R.id.location_2_image_button),
                (ImageButton) findViewById(R.id.location_3_image_button)};
        mTeamChangeButton = (Button) findViewById(R.id.team_change_button);

        mStateTimeTextView = (TextView) findViewById(R.id.state_time_textview);
        mCurrentStateTextView = (TextView) findViewById(R.id.current_state_textview);
        mGpsInfoTextView = (TextView) findViewById(R.id.gps_info_textview);
        mSensorOrientationTextView = (TextView) findViewById(R.id.orientation_textview);
        mGuessXYTextView = (TextView) findViewById(R.id.guess_location_textview);
        mLeftDutyCycleTextView = (TextView) findViewById(R.id.left_duty_cycle_textview);
        mRightDutyCycleTextView = (TextView) findViewById(R.id.right_duty_cycle_textview);
        mMatchTimeTextView = (TextView) findViewById(R.id.match_time_textview);
        mGoOrMissionCompleteButton = (Button) findViewById(R.id.go_or_mission_complete_button);

        mJumbotronXView = (TextView) findViewById(R.id.jumbo_x);
        mJumbotronYView = (TextView) findViewById(R.id.jumbo_y);
        mJumboButton = (Button) findViewById(R.id.jumbo_button);

        dHeadingTime = (TextView) findViewById(R.id.time_since_last_gps_reading_textview);
        dHeadingTime.setText("0");

        mViewFlipper = (ViewFlipper) findViewById(R.id.my_view_flipper);
        mJumboLayout = (LinearLayout) findViewById(R.id.jumbotron_linear_layout);
        
        setState(State.READY_FOR_MISSION);
        setLocationToColor(1, BallColor.NONE); setLocationToColor(2, BallColor.NONE); setLocationToColor(3, BallColor.NONE);
        mScripts = new Scripts(this);
    }

    public void setState(State newState) {
        mStateStartTime = System.currentTimeMillis();
        mCurrentStateTextView.setText(newState.name());
        mJumbotronYView.setText(newState.name());
//        speak(newState.name().replace("_", " "));
        switch (newState) {
            case READY_FOR_MISSION:
                mGoOrMissionCompleteButton.setBackgroundResource(R.drawable.green_button);
                mGoOrMissionCompleteButton.setText("Go!");
                mJumboButton.setBackgroundResource(R.drawable.green_button);
                mJumboButton.setText("G0!");
                sendWheelSpeed(0, 0);
                break;
            case CALIBRATE_BALL_COLORS:
                sendCommand("CUSTOM CALIBRATE_BALLS");
                break;
            case CALIBRATE_STRAIGHT_DRIVING:
                mScripts.testStraightDriveScript();
                break;
            case GO_TO_NEAR_BALL_WITH_GPS:
//                mScripts.goToNearBallScript();
                mViewFlipper.setDisplayedChild(2);
                break;
            case GO_TO_NEAR_BALL_WITH_IMAGE:
                break;
            case DROP_NEAR_BALL:
                sendWheelSpeed(0,0);
                mScripts.nearBallScript();
                break;
            case GO_TO_FAR_BALL_WITH_GPS:
//                mScripts.goToFarBallScript();
                break;
            case GO_TO_FAR_BALL_WITH_IMAGE:
                break;
            case DROP_FAR_BALL:
                sendWheelSpeed(0,0);
                mScripts.farBallScript();
                break;
            case DROP_MID_BALL:
                sendWheelSpeed(0,0);
                mScripts.midBallScript();
            case DRIVE_TOWARDS_HOME_GPS:
//                mScripts.driveTowardsHomeScript();
                break;
            case DRIVE_TOWARDS_HOME_IMAGE:
                break;
            case WAIT_FOR_PICKUP:
                sendWheelSpeed(0, 0);
                break;
            case FIND_HEADING:
                prevState = mState;
                sendWheelSpeed(mLeftStraightPwmValue,mRightStraightPwmValue);
                break;
            case GIVE_UP1:
                updateMissionStrategyVariables();
                mScripts.removeBallAtLocation(mNearBallLocation,State.GIVE_UP2);
                break;
            case GIVE_UP2:
                if (mWhiteBallLocation !=0){
                mScripts.removeBallAtLocation(mWhiteBallLocation,State.GIVE_UP3);
                }else{
                    setState(State.GIVE_UP3);
                }
                break;
            case GIVE_UP3:
                mScripts.removeBallAtLocation(mFarBallLocation,State.GIVE_UP4);
                break;
            case GIVE_UP4:
                mScripts.driveTowardsHomeScript();
        }
        mState = newState;
    }


    /**
     * Use this helper method to set the color of a ball.
     * The location value here is 1 based.  Send 1, 2, or 3
     * Side effect: Updates the UI with the appropriate ball color resource image.
     */
    public void setLocationToColor(int location, BallColor ballColor) {
        mBallImageButtons[location - 1].setImageResource(BALL_DRAWABLE_RESOURCES[ballColor.ordinal()]);
        mLocationColors[location - 1] = ballColor;
    }

    /**
     * Used to get the state time in milliseconds.
     */
    protected long getStateTimeMs() {
        return System.currentTimeMillis() - mStateStartTime;
    }

    /**
     * Used to get the match time in milliseconds.
     */
    private long getMatchTimeMs() {
        return System.currentTimeMillis() - mMatchStartTime;
    }


    /**
     * Used to get the time since the last GPS heading in milliseconds
     */
    private long getLastHeadingTimeMs() {
        return mState==State.READY_FOR_MISSION ? 0: System.currentTimeMillis() - mLastHeadingTime;
    }


    // --------------------------- Methods added ---------------------------


	
	
	
	
	// --------------------------- Drive command ---------------------------
	
	

    // --------------------------- Sensor listeners ---------------------------

    /**
     * GPS sensor updates.
     */
    @Override
    public void onLocationChanged(double x, double y, double heading, Location location) {
        super.onLocationChanged(x, y, heading, location);
        String gpsInfo = getString(R.string.xy_format, x, y);
        if (heading <= 180.0 && heading > -180.0) {
            mCurrHeading = heading;
            gpsInfo += " " + getString(R.string.degrees_format, heading);
            mJumboLayout.setBackgroundColor(Color.parseColor("#00FF00"));
            mLastHeadingTime = System.currentTimeMillis();
        } else {
            gpsInfo += " ?º";
            mJumboLayout.setBackgroundColor(Color.parseColor("#D3D3D3"));
        }
        gpsInfo += "    " + mGpsCounter;
        mGpsInfoTextView.setText(gpsInfo);

    }

    /**
     * Field Orientation sensor updates.
     */
    @Override
    public void onSensorChanged(double fieldHeading, float[] orientationValues) {
        super.onSensorChanged(fieldHeading, orientationValues);
        mSensorOrientationTextView.setText(getString(R.string.degrees_format, fieldHeading));
        mCurrHeading = fieldHeading;
    }

    /** Updates the mission strategy variables. */
    private void updateMissionStrategyVariables() {
        mNearBallLocation = 1;
        mWhiteBallLocation = 0; // Assume there is no white ball present for now (update later).
        mFarBallLocation = 3;

        // Example of doing real planning.
        for (int i = 0; i < 3; i++) {
            BallColor currentLocationsBallColor = mLocationColors[i];
            if (currentLocationsBallColor == BallColor.WHITE) {
                mWhiteBallLocation = i + 1;
            }
            if(mOnRedTeam){
                if(currentLocationsBallColor == BallColor.RED || currentLocationsBallColor == BallColor.GREEN){
                    mNearBallLocation = i+1;
                    if(currentLocationsBallColor ==BallColor.RED){
                        mNearBallGpsY = RIGHT;
                    }else{
                        mNearBallGpsY = LEFT;
                    }
                }else if(currentLocationsBallColor == BallColor.BLUE || currentLocationsBallColor == BallColor.YELLOW){
                    mFarBallLocation = i+1;
                    if(currentLocationsBallColor==BallColor.BLUE){
                        mFarBallGpsY = LEFT;
                    }else{
                        mFarBallGpsY = RIGHT;
                    }
                }
            }else{
                if(currentLocationsBallColor == BallColor.RED || currentLocationsBallColor == BallColor.GREEN){
                    mFarBallLocation = i+1;
                    if(currentLocationsBallColor ==BallColor.RED){
                        mFarBallGpsY = LEFT;
                    }else{
                        mFarBallGpsY = RIGHT;
                    }
                }else if(currentLocationsBallColor == BallColor.BLUE || currentLocationsBallColor == BallColor.YELLOW){
                    mNearBallLocation = i+1;
                    if(currentLocationsBallColor==BallColor.BLUE){
                        mNearBallGpsY = RIGHT;
                    }else{
                        mNearBallGpsY = LEFT;
                    }
                }
            }
        }

        Log.d(TAG, "Near ball is position " + mNearBallLocation + " so drive to " + mNearBallGpsY);
        Log.d(TAG, "Far ball is position " + mFarBallLocation + " so drive to " + mFarBallGpsY);
        Log.d(TAG, "White ball is position " + mWhiteBallLocation);
    }

    // --------------------------- Button Handlers ----------------------------

    /**
     * Helper method that is called by all three golf ball clicks.
     */
    private void handleBallClickForLocation(final int location) {
        new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("What was the real color?").setItems(R.array.ball_colors,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                GolfBallDeliveryActivity.this.setLocationToColor(location, BallColor.values()[which]);
                            }
                        });
                return builder.create();
            }
        }.show(getFragmentManager(), "unused tag");
    }

    public void handleSetState(View view){
        updateMissionStrategyVariables();
        new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("What do you want the state to be?").setItems(R.array.states,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                GolfBallDeliveryActivity.this.setState(State.values()[which]);
                            }
                        });
                return builder.create();
            }
        }.show(getFragmentManager(), "unused tag");
    }

    public void handleGolfBallCalibration(View view){
        setState(State.CALIBRATE_BALL_COLORS);
    }


    /**
     * Click to the far left image button (Location 1).
     */
    public void handleBallAtLocation1Click(View view) {
        handleBallClickForLocation(1);
    }

    /**
     * Click to the center image button (Location 2).
     */
    public void handleBallAtLocation2Click(View view) {
        handleBallClickForLocation(2);
    }

    /**
     * Click to the far right image button (Location 3).
     */
    public void handleBallAtLocation3Click(View view) {
        handleBallClickForLocation(3);
    }

    /**
     * Sets the mOnRedTeam boolean value as appropriate
     * Side effects: Clears the balls
     * @param view
     */
    public void handleTeamChange(View view) {
        setLocationToColor(1, BallColor.NONE);
        setLocationToColor(2, BallColor.NONE);
        setLocationToColor(3, BallColor.NONE);
        if (mOnRedTeam) {
            mOnRedTeam = false;
            mTeamChangeButton.setBackgroundResource(R.drawable.blue_button);
            mTeamChangeButton.setText("Team Blue");
        } else {
            mOnRedTeam = true;
            mTeamChangeButton.setBackgroundResource(R.drawable.red_button);
            mTeamChangeButton.setText("Team Red");
        }
        // setTeamToRed(mOnRedTeam); // This call is optional. It will reset your GPS and sensor heading values.
    }

    /**
     * Sends a message to Arduino to perform a ball color test.
     */
    public void handlePerformBallTest(View view) {
        Toast.makeText(this, "Sent a command to Arduino to perform a ball test.  Waiting for a reply", Toast.LENGTH_SHORT).show();
        sendCommand("CUSTOM COLOR_DETECT");
    }

    /**
     * Clicks to the red arrow image button that should show a dialog window.
     */
    public void handleDrivingStraight(View view) {
        Toast.makeText(this, "handleDrivingStraight", Toast.LENGTH_SHORT).show();
        new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Driving Straight Calibration");
                View dialoglayout = getLayoutInflater().inflate(R.layout.driving_straight_dialog, (ViewGroup) getCurrentFocus());
                builder.setView(dialoglayout);
                final NumberPicker rightDutyCyclePicker = (NumberPicker) dialoglayout.findViewById(R.id.right_pwm_number_picker);
                rightDutyCyclePicker.setMaxValue(255);
                rightDutyCyclePicker.setMinValue(0);
                rightDutyCyclePicker.setValue(mRightStraightPwmValue);
                rightDutyCyclePicker.setWrapSelectorWheel(false);
                final NumberPicker leftDutyCyclePicker = (NumberPicker) dialoglayout.findViewById(R.id.left_pwm_number_picker);
                leftDutyCyclePicker.setMaxValue(255);
                leftDutyCyclePicker.setMinValue(0);
                leftDutyCyclePicker.setValue(mLeftStraightPwmValue);
                leftDutyCyclePicker.setWrapSelectorWheel(false);
                Button doneButton = (Button) dialoglayout.findViewById(R.id.done_button);
                doneButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mLeftStraightPwmValue = leftDutyCyclePicker.getValue();
                        mRightStraightPwmValue = rightDutyCyclePicker.getValue();
                        dismiss();
                    }
                });
                final Button testStraightButton = (Button) dialoglayout.findViewById(R.id.test_straight_button);
                testStraightButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mLeftStraightPwmValue = leftDutyCyclePicker.getValue();
                        mRightStraightPwmValue = rightDutyCyclePicker.getValue();
                        mScripts.testStraightDriveScript();
                    }
                });
                return builder.create();
            }
        }.show(getFragmentManager(), "unused tag");
    }

    public void handleFakeGpsF0(View view) {
        onLocationChanged(0, 0, 0, null); // Midfield
    }

    public void handleFakeGpsF1(View view) {
        onLocationChanged(50, 30, 0, null);  // Out of range so ignored.
    }

    public void handleFakeGpsF2(View view) {
        onLocationChanged(50, 0, 135, null);  // Within range
    }

    public void handleFakeGpsF3(View view) {
        onLocationChanged(240, 41, 35, null);  // Within range
    }

    public void handleFakeGpsH0(View view) {
        onLocationChanged(165, 0, -180, null); // Midfield
    }

    public void handleFakeGpsH1(View view) {
        onLocationChanged(11, 0, -180, null);  // Out of range so ignored.
    }

    public void handleFakeGpsH2(View view) {
        onLocationChanged(9, 0, -170, null);  // Within range
    }

    public void handleFakeGpsH3(View view) {
        onLocationChanged(0, -9, -170, null);  // Within range
    }

    public void handleSetOrigin(View view) {
        mFieldGps.setCurrentLocationAsOrigin();
    }

    public void handleSetXAxis(View view) {
        mFieldGps.setCurrentLocationAsLocationOnXAxis();
    }

    public void handleZeroHeading(View view) {
        mFieldOrientation.setCurrentFieldHeading(0);
    }

    public void handleGiveUp(View vew){setState(State.GIVE_UP1);}

    public void handleGoOrMissionComplete(View view) {
        if (mState == State.READY_FOR_MISSION) {
            mMatchStartTime = System.currentTimeMillis();
            mGoOrMissionCompleteButton.setBackgroundResource(R.drawable.red_button);
            mJumboButton.setBackgroundResource(R.drawable.red_button);
            updateMissionStrategyVariables();
            mGoOrMissionCompleteButton.setText("Mission Complete!");
            mJumboButton.setText("Stop!");

            mCommandHandler.postDelayed(new Runnable() { //start delay of 5 seconds
                @Override
                public void run() {
                    setState(State.GO_TO_NEAR_BALL_WITH_GPS);
                }
            }, 5000);

        } else {
            setState(State.READY_FOR_MISSION);
        }
    }

    @Override
    protected void onCommandReceived(String receivedCommand) {
        super.onCommandReceived(receivedCommand);

        String[] brokenCommand = receivedCommand.split("");

        switch(brokenCommand[2].toUpperCase()){
            case "R":
                setLocationToColor(Integer.parseInt(brokenCommand[1]), BallColor.RED);
                break;
            case "W":
                setLocationToColor(Integer.parseInt(brokenCommand[1]), BallColor.WHITE);
                break;
            case "B":
                setLocationToColor(Integer.parseInt(brokenCommand[1]), BallColor.BLUE);
                break;
            case "Y":
                setLocationToColor(Integer.parseInt(brokenCommand[1]), BallColor.YELLOW);
                break;
            case "G":
                setLocationToColor(Integer.parseInt(brokenCommand[1]), BallColor.GREEN);
                break;
            case "N":
                setLocationToColor(Integer.parseInt(brokenCommand[1]), BallColor.BLACK);
                break;
            case "D":
                setState(State.READY_FOR_MISSION);
                break;
            default:
                setLocationToColor(Integer.parseInt(brokenCommand[1]), BallColor.NONE);
                break;
        }
        updateMissionStrategyVariables();
    }

}
