package com.holgis.net;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ServerSocketFactory;

import okio.BufferedSink;
import okio.Okio;
import okio.Sink;

/**
 * Created by Holger on 23.05.2015.
 */
public class NetServer {

    public static final String TAG = "NetServer";
    private Thread mThread;
    private Context mContext;
    private boolean mStopServer;
    private ServerSocket mServerSocket;
    private NsdHelper mNsdHelper;

    private final Lock mMessageLock = new ReentrantLock();
    private final Condition mMessageReady = mMessageLock.newCondition();

    private List<DistanceMessage> mMessages;

    public void startServer(Context context, int port) {

        mContext = context;
        mStopServer = false;

        mMessages = Collections.synchronizedList(new ArrayList<DistanceMessage>());

        mNsdHelper = new NsdHelper(context);

        try {

            mServerSocket = ServerSocketFactory.getDefault()
                    .createServerSocket(port);

            mThread = new ServerThread(mContext);
            mThread.start();

            mNsdHelper.registerService(port);

        } catch (IOException e) {
            Log.e(TAG, "startServer failed: " + e.getMessage());
        }
    }

    public void stopServer() {

        mNsdHelper.unregisterService();

        try {

            mStopServer = true;
            mMessageLock.lock();
                mMessageReady.signal();
            mMessageLock.unlock();
            mServerSocket.close();

        } catch (IOException e) {
            Log.e(TAG, "stopServer failed: " + e.getMessage());
        }
    }

    public void AddMessage(DistanceMessage message){

        synchronized (mMessages){
            mMessages.add(message);
            if(mMessages.size()>100){
                mMessages.remove(0);
            }
        }
        mMessageLock.lock();
            mMessageReady.signal();
        mMessageLock.unlock();
    }


    public static class DistanceMessage {
        public long Timestamp;
        public float Distance;

        public DistanceMessage(float distance){
            this.Timestamp = System.currentTimeMillis();
            this.Distance = distance;
        }
    }

    private class ServerThread extends Thread {

        private Context mContext;

        public ServerThread(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            super.run();
            try {

                Log.d(TAG, "ServerSocket created:" + mServerSocket
                        .getInetAddress().getHostAddress());

                while (!mStopServer) {
                    Socket socket = mServerSocket.accept();
                    if (!mStopServer) {
                        Worker worker  = new Worker(mContext, socket);
                        worker.start(); // blocking, and closing socket
                    }
                }
            }
            catch (SocketException se) {
                Log.d(TAG, "ServerThread closed: " + se.getMessage());
            }
            catch (IOException e) {
                Log.e(TAG, "ServerThread failed: " + e.getMessage());
            }
        }
    }

    private class Worker {

        private Context mContext;
        private Socket mSocket;

        public Worker(Context context, Socket socket) {
            mContext = context;
            mSocket = socket;
        }

        public void start() {

            try {

                //Source source = Okio.source(mSocket);
                //BufferedSource in = Okio.buffer(source);

                Sink sink = Okio.sink(mSocket);
                BufferedSink out = Okio.buffer(sink);


                while(!mSocket.isClosed()) {

                    mMessageLock.lock();
                    mMessageReady.await();
                    mMessageLock.unlock();

                    if(mStopServer) break;

                    if(!mSocket.isClosed()){

                        boolean isEmpty = false;
                        while(!isEmpty){
                            DistanceMessage message = null;
                            synchronized (mMessages){
                                if(!mMessages.isEmpty()) {
                                    message = mMessages.get(0);
                                    mMessages.remove(0);
                                }
                                isEmpty = mMessages.isEmpty();
                            }

                            if(message != null){

                                out.writeUtf8("Distance: " + message.Distance + "\r\n");

//                                out.writeIntLe(0x12345678);
//                                out.writeLongLe(message.Timestamp);
//                                out.writeIntLe(Float.floatToRawIntBits(message.Distance));
//                                out.writeIntLe(0x87654321);

                                out.flush();
                            }else {
                                isEmpty = true;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "working with mSocket failed: " + e.getMessage());
            }

            try {
                if (!mSocket.isClosed()) {
                    mSocket.close();
                }
            }
            catch (IOException e){
                Log.e(TAG, "mSocket.close failed: " + e.getMessage());
            }
        }
    }
}
