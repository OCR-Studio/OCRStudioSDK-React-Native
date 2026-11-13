/*
  Copyright (c) 2024-2026, OCR Studio
  All rights reserved.
*/

package ai.ocrstudio.sdk.module.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

import ai.ocrstudio.sdk.OCRStudioSDKImage;
import ai.ocrstudio.sdk.OCRStudioSDKPixelFormat;
import ai.ocrstudio.sdk.OCRStudioSDKYUVFormat;

public class Image {

    // Image from base64 string
    public static OCRStudioSDKImage createFromBase64(String imageObject) {
        try {
            return OCRStudioSDKImage.CreateFromBase64FileBuffer(imageObject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Image from Uri
    public static OCRStudioSDKImage createFromUri(String imageObject, Context context) {
        try {
            Bitmap file;
            if (Build.VERSION.SDK_INT >=29 ) {
                file = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), Uri.parse(imageObject)), (imageDecoder, imageInfo, source1) -> imageDecoder.setMutableRequired(true));
            } else {
                file = MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse(imageObject));
            }
            return createFromBitmap(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // image from Bytes. Common Flutter case
    public static OCRStudioSDKImage createFromByteArray(@NonNull byte[] bytes) {
        try {
            return OCRStudioSDKImage.CreateFromFileBuffer(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    // Image from Android Bitmap
    public static OCRStudioSDKImage createFromBitmap(Bitmap imageObject) {
        try {
            // int stride = imageObject.getRowBytes();
            int width = imageObject.getWidth();
            int height = imageObject.getHeight();

            int bytesPerPixel = 4; // Assuming ARGB_8888 format, where each pixel occupies 4 bytes (8 bits per channel)

            int bytesPerLine = width * bytesPerPixel; // Stride
            int bytesPerChannel = bytesPerPixel / 4; // Each channel (A, R, G, B) has 8 bits

            int bufferSize = bytesPerLine * height;
            byte[] byteArray = new byte[bufferSize];

            ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
            imageObject.copyPixelsToBuffer(byteBuffer);

            // Bitmap.getConfig() return ARGB_8888 pixel format. The channel order of ARGB_8888 is RGBA
            return OCRStudioSDKImage.CreateFromPixelBuffer(byteArray, width, height, bytesPerLine, bytesPerChannel, OCRStudioSDKPixelFormat.OCRSTUDIOSDK_PIXEL_FORMAT_RGBA);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    // Image from CameraX
    public static OCRStudioSDKImage createFromImageProxy(ImageProxy imageObject) {
        try {
            int width = imageObject.getWidth(); // ~1280
            int height = imageObject.getHeight(); // ~960

            // YUV_420_888
            if(imageObject.getFormat() == 35) {

                // Example for YUV_FORMAT_420_888
                // According to our tests RGBA_8888 has ~45ms overhead per frame (tested on Helio G90T)
                // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888

                ImageProxy.PlaneProxy planeY = imageObject.getPlanes()[0];
                ImageProxy.PlaneProxy planeU = imageObject.getPlanes()[1];
                ImageProxy.PlaneProxy planeV = imageObject.getPlanes()[2];

                return OCRStudioSDKImage.CreateFromYUV(
                        getByteArrayFromByteBuffer(planeY.getBuffer(), planeY.getRowStride(), height),
                        planeY.getRowStride(), planeY.getPixelStride(),
                        getByteArrayFromByteBuffer(planeU.getBuffer(), planeU.getRowStride(), height),
                        planeU.getRowStride(), planeU.getPixelStride(),
                        getByteArrayFromByteBuffer(planeV.getBuffer(), planeV.getRowStride(), height),
                        planeV.getRowStride(), planeV.getPixelStride(),
                        width, height,
                        OCRStudioSDKYUVFormat.OCRSTUDIOSDK_YUV_FORMAT_420_888);
            }

            // JPG
            if(imageObject.getFormat() == 256) {

                // In case if image.getFormat() return 256 ()
                // Compressed data. Row and pixel strides are 0.
                // https://developer.android.com/reference/android/graphics/ImageFormat#JPEG

                ByteBuffer buffer = imageObject.getPlanes()[0].getBuffer(); // jpg
                byte[] frame_bytes = new byte[buffer.remaining()];
                buffer.get(frame_bytes);
                return OCRStudioSDKImage.CreateFromFileBuffer(frame_bytes);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // ImageProxy to Android Bitmap
    public static Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
        ByteBuffer buffer = planeProxy.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private static byte[] getByteArrayFromByteBuffer(ByteBuffer byteBuffer, int rowStride, int height) {
        // getBuffer() - The stride after the last row may not be mapped into the buffer.
        //  This is why we always calculate the byteBuffer offset.
        //  https://developer.android.com/reference/android/media/Image.Plane#getBuffer()

        int bufferSize = byteBuffer.remaining();
        // The byte array size is stride * height (the leftover spaces will be filled with 0 bytes)
        byte[] bytesArray = new byte[height * rowStride];
        byteBuffer.get(bytesArray, 0, bufferSize);
        return bytesArray;
    }

    private static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(byteBuffer);
        byteBuffer.rewind();
        return byteBuffer.array();
    }

}
