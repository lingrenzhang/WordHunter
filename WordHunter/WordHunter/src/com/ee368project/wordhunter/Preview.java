package com.ee368project.wordhunter;

import com.ee368project.*;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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
import android.view.KeyEvent;
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

	boolean mFocusFlag;
	int mModeFlag;
	boolean mFinished;

	private Context mContext;

	ShutterCallback mShutterCallback;
	PictureCallback mRawCallback;
	PictureCallback mJpegCallback;
	Camera.AutoFocusCallback mAutoFocusCallback;

	private final String SERVERURL = "http://www.stanford.edu/~yzhao3/cgi-bin/ee368/test.php";
	// name for storing image captured by camera view
	private final static String INPUT_IMG_FILENAME = "/temp.jpg";
	private static final String TAG = "WordHunterPreviewClass";

	Preview(Context context, LabelOnTop labelOnTop, int modeFlag) {
		super(context);
		mContext = context;
		mLabelOnTop = labelOnTop;
		mFinished = false;
		mFocusFlag = false;
		mModeFlag = modeFlag;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// register shuttercallback
		mShutterCallback = new ShutterCallback() {
			public void onShutter() {

			}
			// public void onShutter() {
			// try {
			// Thread.sleep(600);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// // Thread.yield();
			// mCamera.startPreview();
			// }
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
				// Start query thread
//				mLabelOnTop.mQueryJpegData = new byte[data.length];
//				System.arraycopy(data, 0, mLabelOnTop.mQueryJpegData, 0,
//						data.length);
//				Thread thread = new Thread(null, mLabelOnTop.mImageUploader,
//						"Background");
//				thread.start();
//
//				// Save image to file
//				try {
//					FileOutputStream fileOutputStream = new FileOutputStream(
//							mLabelOnTop.mImageFilename);
//					fileOutputStream.write(data);
//				} catch (FileNotFoundException exception) {
//					Log.e("debug_log", exception.toString());
//				} catch (IOException exception) {
//					Log.e("debug_log", exception.toString());
//				}

				// // Start query thread
				// Thread thread = new Thread(null, mDrawOnTop.mFileUploader,
				// "Background");
				// thread.start();
				
//				Intent mIntent = new Intent();
  				//compress image
  				compressByteImage(mContext, data, 75);  				
//  				setResult(0, mIntent);
  				
  				//** Send image and offload image processing task  to server by starting async task ** 
  				ServerTask task = new ServerTask();
  				task.execute( Environment.getExternalStorageDirectory().toString() +INPUT_IMG_FILENAME);
  				
  				//start the camera view again .
  				camera.startPreview();  
				
				
			}
		};

		// register a auto-focus call back
		mAutoFocusCallback = new Camera.AutoFocusCallback() {
			public void onAutoFocus(boolean success, Camera camera) {
				if (mFocusFlag == false)
					mFocusFlag = true;
				// SnapWord Mode: modeFlag == true
				// if (mModeFlag == true) {
				// camera.takePicture(mShutterCallback, mRawCallback,
				// mJpegCallback);
				// }

			}
		};
	}

	// store the image as a jpeg image
	public boolean compressByteImage(Context mContext, byte[] imageData,
			int quality) {
		File sdCard = Environment.getExternalStorageDirectory();
		FileOutputStream fileOutputStream = null;

		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 1; // no downsampling
			Bitmap myImage = BitmapFactory.decodeByteArray(imageData, 0,
					imageData.length, options);
			fileOutputStream = new FileOutputStream(sdCard.toString()
					+ INPUT_IMG_FILENAME);

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

	// onKeyDown is used to monitor button pressed and facilitate the autofocus
	// and taking snap
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		// check if the camera button is pressed
		if (keycode == KeyEvent.KEYCODE_CAMERA) {
			// if SnapMode -- why the state variable cannot be used here??
//			if (mModeFlag == 1 && mLabelOnTop.mState == 1) {
//				mLabelOnTop.mState = 0;
//			} else if (mModeFlag == 1 && mFocusFlag == false) {
//				mCamera.takePicture(mShutterCallback, mRawCallback,
//						mJpegCallback);
//			}
			mCamera.takePicture(mShutterCallback, mRawCallback,
					mJpegCallback);
			// no matter what mode, whenever the user touch the screen,
			// set the focusFlag to false, to enable refocus
			mFocusFlag = false;
			return true;
		}
		return super.onKeyDown(keycode, event);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
		try {
			mCamera.setPreviewDisplay(holder);

			// Preview callback is invoked whenever new view frame is
			// available
			mCamera.setPreviewCallback(new PreviewCallback() {
				public void onPreviewFrame(byte[] data, Camera camera) {

					if ((mLabelOnTop == null) || mFinished
							|| (mFocusFlag == false))
						return;
					// only if the camera is focused can we create/update the
					// mBitmap
					if (mLabelOnTop.mBitmap == null) {
						// Initialize the draw-on-top companion
						// If mLabelOnTop.mBitmap == null, means no frame
						// yet,
						// so just create a new bitmap
						Camera.Parameters params = camera.getParameters();
						mLabelOnTop.mImageWidth = params.getPreviewSize().width;
						mLabelOnTop.mImageHeight = params.getPreviewSize().height;
						mLabelOnTop.mBitmap = Bitmap
								.createBitmap(mLabelOnTop.mImageWidth,
										mLabelOnTop.mImageHeight,
										Bitmap.Config.RGB_565);
						mLabelOnTop.mRGBData = new int[mLabelOnTop.mImageWidth
								* mLabelOnTop.mImageHeight];
						mLabelOnTop.mYUVData = new byte[data.length];
					} else {

						// Pass YUV data to draw-on-top companion
						System.arraycopy(data, 0, mLabelOnTop.mYUVData, 0,
								data.length);
						mLabelOnTop.invalidate();

					}
				}
			});

			// Define on touch listener
			// what is the difference between this callback function and the
			// onKeyDown function???
			this.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					mFocusFlag = false;
					// if (mLabelOnTop.mState == LabelOnTop.STATE_ORIGINAL) {
					// mLabelOnTop.mState = LabelOnTop.STATE_PROCESSED;
					// } else if (mLabelOnTop.mState ==
					// LabelOnTop.STATE_PROCESSED) {
					// mLabelOnTop.mState = LabelOnTop.STATE_ORIGINAL;
					// }
					return false;
				}
			});

		} catch (IOException exception) {
			mCamera.release();
			mCamera = null;
		}

	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		mFinished = true;
		mCamera.setPreviewCallback(null);
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Camera.Parameters parameters = mCamera.getParameters();
		parameters.setPreviewSize(320, 240);
		parameters.setPreviewFrameRate(15);
		parameters.setSceneMode(Camera.Parameters.SCENE_MODE_NIGHT);
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		mCamera.setParameters(parameters);
		mCamera.startPreview();
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
				URL url = new URL(SERVERURL);
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
			InputStream is;
			try {
				is = conn.getInputStream();
				// get result image from server
				mLabelOnTop.mBitmap = BitmapFactory.decodeStream(is);
				is.close();
				mLabelOnTop.mState = 1; //STATE_SNAP_MODE
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				e.printStackTrace();
			}
		}

		// Main code for processing image algorithm on the server

		void processImage(String inputImageFilePath) {
			publishProgress(UPLOADING_PHOTO_STATE);
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
			this.dialog.setMessage("Photo captured");
			this.dialog.show();
		}

		@Override
		protected Void doInBackground(String... params) { // background
															// operation
			String uploadFilePath = params[0];
			processImage(uploadFilePath);
			// release camera when previous image is processed
			mFocusFlag = true;
			return null;
		}

		// progress update, display dialogs
		@Override
		protected void onProgressUpdate(Integer... progress) {
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

		@Override
		protected void onPostExecute(Void param) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
		}
	}

}
