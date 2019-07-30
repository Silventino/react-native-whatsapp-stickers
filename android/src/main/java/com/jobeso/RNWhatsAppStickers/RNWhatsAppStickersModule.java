
package com.jobeso.RNWhatsAppStickers;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;



public class RNWhatsAppStickersModule extends ReactContextBaseJavaModule {
  public static final String EXTRA_STICKER_PACK_ID = "sticker_pack_id";
  public static final String EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority";
  public static final String EXTRA_STICKER_PACK_NAME = "sticker_pack_name";

  public static final int ADD_PACK = 200;
  public static final String ERROR_ADDING_STICKER_PACK = "Could not add this sticker pack. Please install the latest version of WhatsApp before adding sticker pack";

  private final ReactApplicationContext reactContext;

  public RNWhatsAppStickersModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNWhatsAppStickers";
  }

  @ReactMethod
  public void test(Promise promise){
    promise.resolve("");
  }

  public static String getContentProviderAuthority(Context context){
    return context.getPackageName() + ".stickercontentprovider";
  }

  @ReactMethod
  public void isPackageInstalled(String packageName, Promise promise) {
    boolean result = WhitelistCheck.isWhitelisted(this.reactContext, packageName);
    promise.resolve(result);
  }

  @ReactMethod
  public void send(String identifier, String stickerPackName, Promise promise) {
    Intent intent = new Intent();
    intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
    intent.putExtra(EXTRA_STICKER_PACK_ID, identifier);
    intent.putExtra(EXTRA_STICKER_PACK_AUTHORITY, getContentProviderAuthority(reactContext));
    intent.putExtra(EXTRA_STICKER_PACK_NAME, stickerPackName);

    try {
      Activity activity = getCurrentActivity();
      ResolveInfo should = activity.getPackageManager().resolveActivity(intent, 0);
      if (should != null) {
        activity.startActivityForResult(intent, ADD_PACK);
        promise.resolve("OK");
      } else {
        promise.resolve("OK, but not opened");
      }
    } catch (ActivityNotFoundException e) {
      promise.reject(ERROR_ADDING_STICKER_PACK, e);
    } catch  (Exception e){
      promise.reject(ERROR_ADDING_STICKER_PACK, e);
    }
  }

  @ReactMethod
  public void insert(String identifier, Promise promise){
    File f = new File(Constants.STICKERS_DIRECTORY_PATH + identifier);
    promise.resolve(f.listFiles());
  } 


  private void saveStickerPack(List<Uri> uries, String name, String author) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Wait a moment while we process your stickers..."); // Setting Message
        progressDialog.setTitle("Processing images"); // Setting Title
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
        progressDialog.show(); // Display Progress Dialog
        progressDialog.setCancelable(false);
        try {

            Intent intent = new Intent(NewStickerPackActivity.this, StickerPackDetailsActivity.class);
            intent.putExtra(StickerPackDetailsActivity.EXTRA_SHOW_UP_BUTTON, true);

            String identifier = "." + FileUtils.generateRandomIdentifier();
            StickerPack stickerPack = new StickerPack(identifier, name, author, Objects.requireNonNull(uries.toArray())[0].toString(), "", "", "", "");

            //Save the sticker images locally and get the list of new stickers for pack
            List<Sticker> stickerList = StickerPacksManager.saveStickerPackFilesLocally(stickerPack.identifier, uries, NewStickerPackActivity.this);
            stickerPack.setStickers(stickerList);

            //Generate image tray icon
            String stickerPath = Constants.STICKERS_DIRECTORY_PATH + identifier;
            String trayIconFile = FileUtils.generateRandomIdentifier() + ".png";
            StickerPacksManager.createStickerPackTrayIconFile(uries.get(0), Uri.parse(stickerPath + "/" + trayIconFile), NewStickerPackActivity.this);
            stickerPack.trayImageFile = trayIconFile;

            //Save stickerPack created to write in json
            StickerPacksManager.stickerPacksContainer.addStickerPack(stickerPack);
            StickerPacksManager.saveStickerPacksToJson(StickerPacksManager.stickerPacksContainer);
            insertStickerPackInContentProvider(stickerPack);

            //Start new activity with stickerpack information
            intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_DATA, stickerPack);
            startActivity(intent);
            NewStickerPackActivity.this.finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
        progressDialog.dismiss();
    }

}
