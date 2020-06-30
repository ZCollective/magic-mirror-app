package org.zcollective.mirrorconfiger.mirrordevicelist;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.WindowDecorActionBar;

import org.zcollective.mirrorconfiger.mirrordevicelist.MirrorDevice;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class MirrorDevicesHandler {

    // TODO: more resource-intense, but more reliable aging: Start a timer for every element and reset it, if it is added again

    public interface DevicesChangedListener {
        void itemRangeRemoved(int position, int amount);
        void itemRemoved(int position);
        void itemAdded(int position);
    }

    private static final String LOG_TAG = "MirrorDevicesHandler";

    // Holds the actual device info
    private final List<MirrorDevice> devices = new ArrayList<>();

    // Holds the ServiceName of the Mirror -> Guaranteed to be unique by our production setup, u
//    private final List<String> deviceMapping = new ArrayList<>();

    private final DevicesChangedListener callback;
    private final HandlerThread handlerThread;
    private final Messenger messenger;

    // TODO: check if we need the lock
    private final Lock lock = new ReentrantLock(true);

    @IntDef(value = {
            DeviceListActions.ADD_ELEMENT,
            DeviceListActions.REMOVE_ELEMENT,
            DeviceListActions.REMOVE_ALL_ELEMENTS,
            DeviceListActions.AGE_ELEMENT,
            DeviceListActions.AGE_ELEMENTS,
    })
    @Retention(value = RetentionPolicy.SOURCE)
    private @interface DeviceListActions {
        int ADD_ELEMENT = 0;
        int REMOVE_ELEMENT = 1;
        int REMOVE_ALL_ELEMENTS = 2;
        int AGE_ELEMENT = 3;
        int AGE_ELEMENTS = 4;
    }

    MirrorDevicesHandler(@NonNull DevicesChangedListener changeListener) {
        callback = changeListener;
        handlerThread = new HandlerThread(LOG_TAG);
        handlerThread.setDaemon(true);
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper()) {

            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.i(LOG_TAG, "MirrorDevicesHandler::handleMessage(msg.what=" + msg.what + ")");

                switch (msg.what) {
                    case DeviceListActions.ADD_ELEMENT:
                        if (!(msg.obj instanceof MirrorDevice)) return;
                        addItemAndCallBack((MirrorDevice) msg.obj);
                        break;
                    case DeviceListActions.REMOVE_ELEMENT:
                        if (!(msg.obj instanceof MirrorDevice)) return;
                        removeItemAndCallBack((MirrorDevice) msg.obj);
                        break;
                    case DeviceListActions.REMOVE_ALL_ELEMENTS:
                        if (devices.size() == 0) return;
                        removeAllItemsAndCallBack();
                        break;
                    case DeviceListActions.AGE_ELEMENT:
                        if (!(msg.obj instanceof MirrorDevice)) return;
                        ageItem((MirrorDevice) msg.obj);
                        break;
                    case DeviceListActions.AGE_ELEMENTS:
                        age();
                        break;
                    default:
                        Log.wtf(LOG_TAG, "MirrorDevicesHandler::handleMessage(msg.what=" + msg.what + ") is unknown");
                        break;
                }
            }
        };
        messenger = new Messenger(handler);
    }

    public void destroy() {
        handlerThread.getLooper().quitSafely();
        handlerThread.quitSafely();
    }

    void addElement(@Nullable MirrorDevice device) throws RemoteException {
        if (device == null) return;

        Message message = new Message();
        message.what = DeviceListActions.ADD_ELEMENT;
        message.obj = device;
        messenger.send(message);
    }

    void removeElement(@Nullable MirrorDevice device) throws RemoteException {
        if (device == null) return;

        Message message = new Message();
        message.what = DeviceListActions.REMOVE_ELEMENT;
        message.obj = device;
        messenger.send(message);
    }

    void ageElement(@Nullable MirrorDevice device) throws RemoteException {
        if (device == null) return;

        Message message = new Message();
        message.what = DeviceListActions.AGE_ELEMENT;
        message.obj = device;
        messenger.send(message);
    }

    void ageElements() throws RemoteException {
        Message message = new Message();
        message.what = DeviceListActions.AGE_ELEMENTS;
        messenger.send(message);
    }

    void clear() throws RemoteException {
        Message message = new Message();
        message.what = DeviceListActions.REMOVE_ALL_ELEMENTS;
        messenger.send(message);
    }

    private void addItemAndCallBack(@NonNull MirrorDevice device) {
        int posAdd = add(device);
        if (posAdd >= 0) callback.itemAdded(posAdd);
    }

    private void removeItemAndCallBack(@NonNull MirrorDevice device) {
        int posRemove = remove(device);
        if (posRemove >= 0) callback.itemRemoved(posRemove);
    }

    private void removeAllItemsAndCallBack() {
        lock.lock();
        try{
            int deviceAmount = devices.size();
            devices.clear();
            callback.itemRangeRemoved(0, deviceAmount);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private void ageItem(@NonNull MirrorDevice mirrorDevice) {
        lock.lock();
        try{
            for (MirrorDevice device : devices) {
                if (device.serviceInfo.getServiceName().equals(mirrorDevice.serviceInfo.getServiceName())){
                    device.markedForRemoval.set(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }

    private void age() {
        lock.lock();
        try{
            Log.i(LOG_TAG, "MirrorDevicesHandler::ageElements()");
            long start = System.currentTimeMillis();
            for (MirrorDevice device : devices) {
                if (device.markedForRemoval.get()) {
                    removeItemAndCallBack(device);
                } else {
                    device.markedForRemoval.set(true);
                }
            }
            Log.i(LOG_TAG, "MirrorDevicesHandler::ageElements() finished. duration=" + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private int add(@NonNull MirrorDevice mirrorDevice) {
        lock.lock();
        try{
            Log.i(LOG_TAG, "MirrorDevicesHandler::add()");
            /*
            For inserting we have two main scenarios:
            The device already exists -> Checked by comparing getServiceName()
            The device does not exist yet -> Search a unique visiblename
             */

            Log.d(LOG_TAG, "Currently listed devices: " + devices.toString());
            Log.d(LOG_TAG, "Trying to insert device: " + mirrorDevice.toString());

            for(MirrorDevice device : devices) {
                if (mirrorDevice.serviceInfo.getServiceName().equals(device.serviceInfo.getServiceName())) {
                    // Device is already listed. Removing age status and returning mirror id
                    device.markedForRemoval.set(false);
                    //Returning -1, because we did not actually add an item
                    return -1;
                }
            }
            devices.add(mirrorDevice);
            return devices.size() -1;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return -1;
    }

    private int remove(@Nullable MirrorDevice mirrorDevice) {
        lock.lock();
        try{
            Log.i(LOG_TAG, "MirrorDevicesHandler::remove()");
            if (mirrorDevice == null) return -1;
            boolean exists = false;

            // Initializing deviceToRemove to trick the intellij sense
            MirrorDevice deviceToRemove = mirrorDevice;
            int index = -1;
            for(MirrorDevice device : devices) {
                if (device.serviceInfo.getServiceName().equals(mirrorDevice.serviceInfo.getServiceName())) {
                    exists = true;
                    deviceToRemove = device;
                    index = devices.indexOf(device);
                    break;
                }
            }
            if (!exists) return -1;
            else {
                devices.remove(deviceToRemove);
                return index;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return -1;
    }

    MirrorDevice get(@IntRange(from = 0) int index) {
        try {
            lock.lock();
            return devices.get(index);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return null;
    }

    private boolean nameExists(String name) {
        for(MirrorDevice device : devices) {
            if (device.visibleName.equals(name)) return true;
        }
        return false;
    }
}
