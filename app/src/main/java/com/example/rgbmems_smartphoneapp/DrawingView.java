package com.example.rgbmems_smartphoneapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

public class DrawingView extends View {

    // Global Variables
    private Path drawPath; // Path for drawing
    private Paint drawPaint, erasePaint, canvasPaint; // Paint objects for drawing, erasing, and canvas
    private Bitmap canvasBitmap; // Bitmap for the canvas
    private Canvas drawCanvas; // Canvas to draw on the bitmap
    private float brushSize = 10;  // Size for drawing brush
    private float eraserSize = 50; // Size for eraser
    private static final Stack<Bitmap> undoStack = new Stack<>(); // Stack for undo operations
    private static final Stack<Bitmap> redoStack = new Stack<>(); // Stack for redo operations

    private int ToolMode = SecondFragment.TOOL_NEUTRAL;

    // Bitmap for background image
    private Bitmap backgroundBitmap;


    // Initialization
    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing(); // Call method to setup drawing properties
    }

    // Setup drawing properties
    private void setupDrawing() {
        drawPath = new Path(); // Initialize drawing path

// Setup draw paint for drawing
        drawPaint = new Paint();
        drawPaint.setColor(Color.BLACK);
        drawPaint.setAntiAlias(true); // Enable anti-aliasing for smooth edges
        drawPaint.setStrokeWidth(brushSize); // Set brush size
        drawPaint.setStyle(Paint.Style.STROKE); // Set paint style to stroke
        drawPaint.setStrokeJoin(Paint.Join.ROUND); // Set stroke join style
        drawPaint.setStrokeCap(Paint.Cap.ROUND); // Set stroke cap style

// Setup erase paint for erasing
        erasePaint = new Paint();
        erasePaint.setAntiAlias(true); // Enable anti-aliasing for erase paint
        erasePaint.setStrokeWidth(brushSize); // Set brush size for eraser
        erasePaint.setStyle(Paint.Style.FILL); // Set paint style to FILL
        erasePaint.setStrokeJoin(Paint.Join.ROUND); // Set stroke join style
        erasePaint.setStrokeCap(Paint.Cap.ROUND); // Set stroke cap style
        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));  // Set mode to clear for erasing

        canvasPaint = new Paint(Paint.DITHER_FLAG); // Paint for canvas
    }

    public Bitmap getBitmapFromDrawingView() {
// Assuming you have drawn on the canvas and want to get Bitmap from it
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas); // Draw content onto the canvas
        return bitmap;
    }

    // Called when the size of the view changes
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); // Create a bitmap for the canvas
        drawCanvas = new Canvas(canvasBitmap); // Create a canvas from the bitmap
    }

    public void setPaintColor(int color) {
        drawPaint.setColor(color);
        invalidate();
    }

    // Draw the canvas and paths
    @Override
    protected void onDraw(Canvas canvas) {
        if (backgroundBitmap != null) {
// Get dimensions of the background bitmap
            int bitmapWidth = backgroundBitmap.getWidth();
            int bitmapHeight = backgroundBitmap.getHeight();
            float viewWidth = getWidth();
            float viewHeight = getHeight();

// Calculate scale to fit the bitmap in the view
            float scale = Math.min(viewWidth / bitmapWidth, viewHeight / bitmapHeight);
            int newWidth = (int) (bitmapWidth * scale);
            int newHeight = (int) (bitmapHeight * scale);

// Calculate position to center the bitmap
            int left = (int) ((viewWidth - newWidth) / 2);
            int top = (int) ((viewHeight - newHeight) / 2);

// Draw the background bitmap on the canvas
            canvas.drawBitmap(backgroundBitmap, null, new Rect(left, top, left + newWidth, top + newHeight), null);
        }

// Draw the canvas bitmap and current path
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        if (ToolMode == SecondFragment.TOOL_ERASER){            // 消しゴムの場合
            canvas.drawPath(drawPath, erasePaint);
        } else if (ToolMode == SecondFragment.TOOL_NEUTRAL) {    // ニュートラル状態の場合
// 描画ツールがactiveではないため設定しない。
        } else {                        // 消しゴム以外の描画ツールの場合
            canvas.drawPath(drawPath, drawPaint);
        }
    }
    private CustomViewPager customViewPager;

    // Handle touch events for drawing and erasing
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX(); // Get touch X coordinate
        float touchY = event.getY(); // Get touch Y coordinate


        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (ToolMode == SecondFragment.TOOL_NEUTRAL) {
// 何もしない
                } else if (ToolMode == SecondFragment.TOOL_ERASER) {
// Save the current state before erasing
                    saveCanvasState();
// Directly use erase paint for erasing
                    drawCanvas.drawCircle(touchX, touchY, eraserSize, erasePaint);
                } else if (ToolMode == SecondFragment.TOOL_BLACK_PEN) {
                    saveCanvasState();  // Save current canvas state for drawing
                    drawPath.moveTo(touchX, touchY); // Move to touch position
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (ToolMode == SecondFragment.TOOL_NEUTRAL) {
// 何もしない
                } else if (ToolMode == SecondFragment.TOOL_ERASER) {
// Continue erasing
                    drawCanvas.drawCircle(touchX, touchY, eraserSize, erasePaint);
                } else if (ToolMode == SecondFragment.TOOL_BLACK_PEN) {
                    drawPath.lineTo(touchX, touchY); // Draw line to touch position
                }
                break;

            case MotionEvent.ACTION_UP:
                if (ToolMode == SecondFragment.TOOL_NEUTRAL) {
// 何もしない
                } else if (ToolMode == SecondFragment.TOOL_ERASER) {
// End erasing
                    drawCanvas.drawCircle(touchX, touchY, eraserSize, erasePaint);
                } else if (ToolMode == SecondFragment.TOOL_BLACK_PEN) {
                    drawPath.lineTo(touchX, touchY); // Finalize the line
                    drawCanvas.drawPath(drawPath, drawPaint);  // Draw the path with appropriate paint
                }
                drawPath.reset(); // Reset the path for the next drawing
                break;

            default:
                return false; // Handle unrecognized touch events
        }

        invalidate(); // Request to redraw the view
        return true; // Indicate that the event was handled
    }

    // Save canvas state before changes (used for undo/redo)
    public void saveCanvasState() {
        Bitmap saveBitmap = canvasBitmap.copy(Bitmap.Config.ARGB_8888, true); // Copy current canvas bitmap
        undoStack.push(saveBitmap); // Push current state to undo stack
        redoStack.clear();  // Clear redo stack when a new action occurs
    }

    // Undo method
    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(canvasBitmap.copy(Bitmap.Config.ARGB_8888, true)); // Save current state to redo stack
            canvasBitmap = undoStack.pop(); // Get the last saved state from undo stack
            drawCanvas.setBitmap(canvasBitmap);  // Update drawCanvas with new bitmap
            invalidate(); // Request to redraw the view
        }
    }

    // Redo method
    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(canvasBitmap.copy(Bitmap.Config.ARGB_8888, true)); // Save current state to undo stack
            canvasBitmap = redoStack.pop(); // Get the last saved state from redo stack
            drawCanvas.setBitmap(canvasBitmap);  // Update drawCanvas with new bitmap
            invalidate(); // Request to redraw the view
        }
    }

    // Set ToolMode
    public void setToolMode(int isToolMode) {
        ToolMode = isToolMode;
    }
    // Set brush size
// Method to set brush thickness
    public void setBrushThickness(float thickness) {
        this.brushSize = thickness;
        drawPaint.setStrokeWidth(thickness);
        erasePaint.setStrokeWidth(thickness);
        invalidate();
    }


    // Method to set background bitmap
    public void setBackgroundBitmap(Bitmap bitmap) {
        this.backgroundBitmap = bitmap; // Set background bitmap
        invalidate(); // Request to redraw the view to display the background
    }

    // Method to load an image from Uri
    public void loadImage(Uri imageUri) {
        try {
// Convert URI to Bitmap
            InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

// Get the size of the DrawingView
            int viewWidth = getWidth();
            int viewHeight = getHeight();

// Calculate the aspect ratio of the image and the aspect ratio of the DrawingView
            float imageRatio = (float) bitmap.getWidth() / bitmap.getHeight();
            float viewRatio = (float) viewWidth / viewHeight;

            int newWidth, newHeight;

// If the image ratio is greater than the DrawingView ratio, use the width of the DrawingView
            if (imageRatio > viewRatio) {
                newWidth = viewWidth;
                newHeight = Math.round(viewWidth / imageRatio);
            } else { // If the DrawingView ratio is greater than or equal to the image ratio, use the height of the DrawingView
                newHeight = viewHeight;
                newWidth = Math.round(viewHeight * imageRatio);
            }

// Resize the image to fit the DrawingView without distortion
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

// Set the background
            setBackgroundBitmap(resizedBitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        if (drawCanvas != null) {
// Clear the canvas by drawing with a transparent color
            drawCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

// Set backgroundBitmap to null to have no background image
            backgroundBitmap = null;

// Make the DrawingView redisplay (call onDraw)
            invalidate();
        }
    }

    public Bitmap getBitmap() {
// Create a new bitmap to save the background and the drawn strokes
        Bitmap combinedBitmap = Bitmap.createBitmap(canvasBitmap.getWidth(), canvasBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas combinedCanvas = new Canvas(combinedBitmap);

// Draw the background onto the new bitmap
        if (backgroundBitmap != null) {
            combinedCanvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }

// Draw the canvas onto the new bitmap
        combinedCanvas.drawBitmap(canvasBitmap, 0, 0, null);

        return combinedBitmap; // Return a bitmap that contains both the background and the strokes
    }

    public void saveImage(Context context) {
// Create a bitmap from the canvasBitmap
        Bitmap bitmap = getBitmap();

// Create a filename for the image
        String filename = "Drawing_" + System.currentTimeMillis() + ".png";

// Save the image to the gallery
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp"); // Thay đổi MyApp thành tên thư mục bạn muốn

// Save the image to MediaStore
            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (FileOutputStream outputStream = (FileOutputStream) context.getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
// Notify the system that the data has changed
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bitmap.recycle(); // Release the bitmap
        }
    }
}































































































































































































































































































































































































































































































































































































































































































