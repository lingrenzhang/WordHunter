package com.ee368project.wordhunter;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


/**
 * @author Yang Zhao
 * 
 */
public class Preview extends SurfaceView implements SurfaceHolder.Callback {
	SurfaceHolder mHolder;
	Camera mCamera;
	LabelOnTop mLabelOnTop;

	int mOperationMode;
	boolean mFocusFlag;
	String wordToSearch;
	int scanState;
	

	private Context mContext;

	ShutterCallback mShutterCallback;
	PictureCallback mRawCallback;
	PictureCallback mJpegCallback;
	Camera.AutoFocusCallback mAutoFocusCallback;
	int scanModeProcessing;

	private final String SERVERURL = "http://www.stanford.edu/~yzhao3/cgi-bin/ee368/searchWordOnCorn.php";
//	private final String SERVERURL = "http://www.stanford.edu/~shuol/cgi-bin/searchWordOnCorn.php";
	
	// name for storing image captured by camera view
	private final static String INPUT_IMG_FILENAME = "/temp.jpg";
//	static int SCAN_INIT = 0;
//	static int SCAN_AUTOFOCUS_BEGIN = 1;
//	static int SCAN_AUTOFOCUS_IN_PROGRESS = 2;
//	static int SCAN_PROCESS_BEGIN = 3;
//	static int SCAN_PROCESS_IN_PROGRESS = 4;
	
	//state machine
	private final static int SCAN_IN_PREVIEW = 0;
	private final static int SCAN_IN_PROCESS = 1;
	
	
	//Jason's camera
	static int mWidthPreview = 960;
	static int mHeightPreview = 640;
	static int mWidthPicture = 1280;
	static int mHeightPicture = 960;
	//Shuo's camera
//	static int mWidthPreview = 1024;
//	static int mHeightPreview = 768;
//	static int mWidthPicture = 1024;
//	static int mHeightPicture = 768;
	
	private static final String TAG = "WordHunterPreviewClass";
	private static final String LOG_TAG = "Android Debug Log";
	

	Preview(Context context, LabelOnTop labelOnTop, int modeFlag, String message) {
		
		super(context);
//		scanState = SCAN_AUTOFOCUS_BEGIN;
		scanState = SCAN_IN_PREVIEW;
		mContext = context;
		mLabelOnTop = labelOnTop;
		mFocusFlag = false;
		mOperationMode = modeFlag;
		wordToSearch = message;
		scanModeProcessing = 0;
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		Log.d(TAG, "entering Preview constructor");
		
		// register shuttercallback
		mShutterCallback = new ShutterCallback() {
			public void onShutter() {
			}
		};

		// Handles data for raw picture
		mRawCallback = new PictureCallback() {
			@Override
			public void onPictureTaken(byte[] arg0, android.hardware.Camera arg1) {
			}
		};

		// Define jpeg callback
		mJpegCallback = new PictureCallback() {
			public void onPictureTaken(byte[] data, Camera camera) {
				Log.d(TAG, "Picture callback");
  				//compress image
				if (mOperationMode == ScanWordActivity.SCAN_MODE) {
//					scanState = SCAN_PROCESS_IN_PROGRESS;
					scanState = SCAN_IN_PROCESS;
				}
  				compressByteImage(mContext, data, 75);  				
  				
  				//** Send image and offload image processing task  to server by starting async task ** 
  				ServerTask task = new ServerTask();
  				task.execute( Environment.getExternalStorageDirectory().toString() +INPUT_IMG_FILENAME);
  				
  				//start the camera view again .
  				camera.startPreview();  
			}
		};

		// register an auto-focus call back
		mAutoFocusCallback = new Camera.AutoFocusCallback() {
			public void onAutoFocus(boolean success, Camera camera) {
				Log.d(TAG, "I'm already in the onAutoFocus callback");
				if (mOperationMode == SnapWordActivity.SNAP_MODE) {
					Log.d(TAG, "Snap mode, onAutoFocus doesn't need take a picture");
				} else if (mOperationMode == ScanWordActivity.SCAN_MODE) {
					camera.takePicture(mShutterCallback, mRawCallback,
							mJpegCallback);
  					//scanState = SCAN_PROCESS_BEGIN;
				}
				//after auto-focus, set the mFocusFlag to true
				if (success == true && mFocusFlag == false)
					mFocusFlag = true;
			}
		};
	}

	
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
		try {
			mCamera.setPreviewDisplay(holder);

			// Preview callback is invoked whenever new view frame is
			// available
			mCamera.setPreviewCallback(new PreviewCallback() {
				  // Called for each frame previewed		
		        public void onPreviewFrame(byte[] data, Camera camera) {  // <11>
		        	Log.d(TAG, "Entered onPreviewFrame");
//		        	if (scanState == SCAN_AUTOFOCUS_BEGIN) {
//						Log.d(TAG, "in onPreviewFrame, state = SCAN_AUTOFOCUS_BEGIN");
//					}else if (scanState == SCAN_AUTOFOCUS_IN_PROGRESS) {
//						Log.d(TAG, "in onPreviewFrame, state = SCAN_AUTOFOCUS_IN_PROGRESS");
//					}else if (scanState == SCAN_PROCESS_BEGIN) {
//						Log.d(TAG, "in onPreviewFrame, state = SCAN_PROCESS_BEGIN");
//					} else if (scanState == SCAN_PROCESS_IN_PROGRESS) {
//						Log.d(TAG, "in onPreviewFrame, state = SCAN_PROCESS_IN_PROGRESS");
//					}
		        	
					if (mOperationMode == ScanWordActivity.SCAN_MODE) {
						if (scanState == SCAN_IN_PREVIEW) {
							mCamera.autoFocus(mAutoFocusCallback);
							scanState = SCAN_IN_PROCESS;
						//} else if (scanState == SCAN_PROCESS_BEGIN) {
							//doesn't need to do anything   	
							  
						}
					}
					//invalid the preview so that we can draw layer on top of it
					Preview.this.invalidate();
		        }
		      });
		   

			// Define on touch listener
			this.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					mCamera.autoFocus(mAutoFocusCallback);
					return false;
				}
			});

		} catch (IOException exception) {
			mCamera.release();
			mCamera = null;
		}

	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Camera.Parameters parameters = mCamera.getParameters();

		//Jason's camera
		//  parameters.setPreviewSize(960, 640);
		    parameters.setPictureSize(1280, 960);
		 
		//Shuo's camera
		//    parameters.setPictureSize(1024, 768);
	
		List<Camera.Size> picSizes = parameters.getSupportedPictureSizes();
		List<Camera.Size> previewSize = parameters.getSupportedPreviewSizes();
		Log.d(LOG_TAG, "hello everybody\n");
		List<String> focusModes = parameters.getSupportedFocusModes();
		
		for (Camera.Size size : picSizes) {
			Log.d(LOG_TAG, "supported picture size: height: " + size.height + " width:  " + size.width);
		}
		for (Camera.Size size : picSizes) {
			Log.d(LOG_TAG, "supported preview size: height: " + size.height + " width:  " + size.width);
		}
		for (String mode : focusModes) {
			Log.d(LOG_TAG, "supported focus modes: " + mode);
		}
		Log.d(LOG_TAG, "Picture format is " + parameters.getPictureFormat());
		parameters.setPreviewFrameRate(15);
//		parameters.setSceneMode(Camera.Parameters.SCENE_MODE_NIGHT);
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		mCamera.setParameters(parameters);
		mCamera.startPreview();
	}
	
    /*static public void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
    	final int frameSize = width * height;
    	
    	for (int j = 0, yp = 0; j < height; j++) {
    		int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
    		for (int i = 0; i < width; i++, yp++) {
    			int y = (0xff & ((int) yuv420sp[yp])) - 16;
    			if (y < 0) y = 0;
    			if ((i & 1) == 0) {
    				v = (0xff & yuv420sp[uvp++]) - 128;
    				u = (0xff & yuv420sp[uvp++]) - 128;
    			}
    			
    			int y1192 = 1192 * y;
    			int r = (y1192 + 1634 * v);
    			int g = (y1192 - 833 * v - 400 * u);
    			int b = (y1192 + 2066 * u);
    			
    			if (r < 0) r = 0; else if (r > 262143) r = 262143;
    			if (g < 0) g = 0; else if (g > 262143) g = 262143;
    			if (b < 0) b = 0; else if (b > 262143) b = 262143;
    			
    			rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    		}
    	}
    }*/
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		mCamera.setPreviewCallback(null);
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}
	
	// store the image as a jpeg image
	public boolean compressByteImage(Context mContext, byte[] imageData,
			int quality) {
		File sdCard = Environment.getExternalStorageDirectory();
		FileOutputStream fileOutputStream = null;
		if (mOperationMode == SnapWordActivity.SNAP_MODE) {
			Log.d(TAG, "Entering compress image, SNAP mode");
		} else if (mOperationMode == ScanWordActivity.SCAN_MODE) {
			Log.d(TAG, "Entering compress image, SCAN mode");
		}

		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 1; // no downsampling
			Bitmap myImage = null;
			/*int[] rgbArray = null;
			if (mModeFlag == SnapWordActivity.SNAP_MODE) {
				myImage = BitmapFactory.decodeByteArray(imageData, 0,
						imageData.length, options);
			} else if (mModeFlag == ScanWordActivity.SCAN_MODE) {
				decodeYUV420SP(rgbArray, imageData, mWidthPreview,  mHeightPreview);
				myImage = BitmapFactory.decodeByteArray(rgbArray, 0,
						imageData.length, options);
			}*/
			 myImage = BitmapFactory.decodeByteArray(imageData, 0,
					imageData.length, options);
			fileOutputStream = new FileOutputStream(sdCard.toString()
					+ INPUT_IMG_FILENAME);
			Log.d(TAG, "Image length is " + Integer.toString(imageData.length));
			BufferedOutputStream bos = new BufferedOutputStream(
					fileOutputStream);

			// compress image to jpeg
			myImage.compress(CompressFormat.JPEG, quality, bos);

			bos.flush();
			bos.close();
			fileOutputStream.close();

		} catch (FileNotFoundException e) {
			Log.e(TAG, "FileNotFoundException");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "IOException");
			e.printStackTrace();
		} 
		return true;
	}


	/**
	 * Inner class -- ServerTask, used to upload the image file to the server
	 *  
	 */
	public class ServerTask extends AsyncTask<String, Integer, Void> {
		public byte[] dataToServer;

		// Task state
		private final int UPLOADING_PHOTO_STATE = 0;
		private final int SERVER_PROC_STATE = 1;

		private ProgressDialog dialog;

		public ServerTask() {
			dialog = new ProgressDialog(mContext);
		}

		// upload photo to server
		HttpURLConnection uploadPhoto(FileInputStream fileInputStream) {

			final String serverFileName = "test"
					+ (int) Math.round(Math.random() * 1000) + ".jpg";
			final String lineEnd = "\r\n";
			final String twoHyphens = "--";
			final String boundary = "*****";

			try {
				String urlString = SERVERURL + "?word=" + wordToSearch + "&mode=" + Integer.toString(mOperationMode);
				Log.d(TAG, urlString);
				URL url = new URL(urlString);
				// Open a HTTP connection to the URL
				final HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				// Allow Inputs
				conn.setDoInput(true);
				// Allow Outputs
				conn.setDoOutput(true);
				// Don't use a cached copy.
				conn.setUseCaches(false);

				// Use a post method.
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Connection", "Keep-Alive");
				conn.setRequestProperty("Content-Type",
						"multipart/form-data;boundary=" + boundary);

				DataOutputStream dos = new DataOutputStream(
						conn.getOutputStream());

				dos.writeBytes(twoHyphens + boundary + lineEnd);
				dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\""
						+ serverFileName + "\"" + lineEnd);
				dos.writeBytes(lineEnd);

				// create a buffer of maximum size
				int bytesAvailable = fileInputStream.available();
				int maxBufferSize = 1024;
				int bufferSize = Math.min(bytesAvailable, maxBufferSize);
				byte[] buffer = new byte[bufferSize];

				// read file and write it into form...
				int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

				while (bytesRead > 0) {
					dos.write(buffer, 0, bufferSize);
					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);
				}

				// send multipart form data after file data...
				dos.writeBytes(lineEnd);
				dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
				publishProgress(SERVER_PROC_STATE);
				// close streams
				fileInputStream.close();
				dos.flush();

				return conn;
			} catch (MalformedURLException ex) {
				Log.e(TAG, "error: " + ex.getMessage(), ex);
				return null;
			} catch (IOException ioe) {
				Log.e(TAG, "error: " + ioe.getMessage(), ioe);
				return null;
			}
		}

				
		
		// get image result from server and display it in result view
		void getResultImage(HttpURLConnection conn) {
			// retrieve the response from server
			Log.d(TAG, "getResultImage");
			InputStream is;
			try {
				is = conn.getInputStream();
				// push state change a little earlier
				// from within getResultImage(conn) to before it
				scanState = SCAN_IN_PREVIEW;
				// get result image from server
				mLabelOnTop.mBitmap = BitmapFactory.decodeStream(is);
				mLabelOnTop.setCanvasState(mOperationMode);
				is.close();
				//scanState = SCAN_IN_PREVIEW;
			} catch (IOException e) {
				Log.d(TAG, "not get the result yet!!!!!!!!!!!");
				Log.e(TAG, e.toString());
				e.printStackTrace();
			}
		}
		
		
		// Main code for processing image algorithm on the server

		void processImage(String inputImageFilePath) {
			if (mOperationMode == SnapWordActivity.SNAP_MODE) {
				publishProgress(UPLOADING_PHOTO_STATE);
				Log.d(TAG, "Entering process image, SNAP mode");
			} else if (mOperationMode == ScanWordActivity.SCAN_MODE) {
				Log.d(TAG, "Entering process image, SCAN mode");
			}
				
			
			File inputFile = new File(inputImageFilePath);
			try {
				
				// create file stream for captured image file
				FileInputStream fileInputStream = new FileInputStream(inputFile);

				// upload photo
				final HttpURLConnection conn = uploadPhoto(fileInputStream);

				// get processed photo from server
				if (conn != null) {
					
					getResultImage(conn);
				}
				fileInputStream.close();
			} catch (FileNotFoundException ex) {
				Log.e(TAG, ex.toString());
			} catch (IOException ex) {
				Log.e(TAG, ex.toString());
			}
		}

		protected void onPreExecute() {
			if (mOperationMode == SnapWordActivity.SNAP_MODE) {
				this.dialog.setMessage("Photo captured");
				this.dialog.show();
			}
		}
		

		@Override
		protected Void doInBackground(String... params) { // background
															// operation
			String uploadFilePath = params[0];
			processImage(uploadFilePath);
			// release camera when previous image is processed
			//mFocusFlag = true;
			return null;
		}

		// progress update, display dialogs
		@Override
		protected void onProgressUpdate(Integer... progress) {
			if (mOperationMode == SnapWordActivity.SNAP_MODE) {
				if (progress[0] == UPLOADING_PHOTO_STATE) {
					dialog.setMessage("Uploading");
					dialog.show();
				} else if (progress[0] == SERVER_PROC_STATE) {
					if (dialog.isShowing()) {
						dialog.dismiss();
					}
					dialog.setMessage("Processing");
					dialog.show();
				}
			}
		}

		@Override
		protected void onPostExecute(Void param) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
		}
	}

}
