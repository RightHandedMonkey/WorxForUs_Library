package com.worxforus.android;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import com.worxforus.Base64Support;
import com.worxforus.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;


/**
 * Thanks to emmby at http://stackoverflow.com/questions/785973/what-is-the-most-appropriate-way-to-store-user-settings-in-android-application/6393502#6393502
 * 
 * Simply wrap your own SharedPreferences object in this one, and any data you read/write will be automatically encrypted and decrypted. eg.
 * Usage:
 * final SharedPreferences prefs = new ObscuredSharedPreferences( 
 *   this, this.getSharedPreferences(MY_PREFS_FILE_NAME, Context.MODE_PRIVATE) );
 *
 *	// eg.    
 *	prefs.edit().putString("foo","bar").commit();
 *	prefs.getString("foo", null);
 *
 * Warning, this gives a false sense of security.  If an attacker has enough access to
 * acquire your password store, then he almost certainly has enough access to acquire your
 * source binary and figure out your encryption key.  However, it will prevent casual
 * investigators from acquiring passwords, and thereby may prevent undesired negative
 * publicity.
 */
public class ObscuredSharedPreferences implements SharedPreferences {
    protected static final String UTF8 = Utils.CHARSET; //"utf-8";
    private static final char[] SEKRIT = { 'a', 'b', 'c', 'd', 'e' } ; // INSERT A RANDOM PASSWORD HERE.
                                               // Don't use anything you wouldn't want to
                                               // get out there if someone decompiled
                                               // your app.


    protected SharedPreferences delegate;
    protected Context context;

    public ObscuredSharedPreferences(Context context, SharedPreferences delegate) {
        this.delegate = delegate;
        this.context = context;
    }

    public class Editor implements SharedPreferences.Editor {
        protected SharedPreferences.Editor delegate;

        public Editor() {
            this.delegate = ObscuredSharedPreferences.this.delegate.edit();                    
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            delegate.putString(key, encrypt(Boolean.toString(value)));
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            delegate.putString(key, encrypt(Float.toString(value)));
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            delegate.putString(key, encrypt(Integer.toString(value)));
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            delegate.putString(key, encrypt(Long.toString(value)));
            return this;
        }

        @Override
        public Editor putString(String key, String value) {
            delegate.putString(key, encrypt(value));
            return this;
        }

        @Override
        public void apply() {
        	//to maintain compatibility with android level 7 
        	delegate.commit();
//            delegate.apply();
        }

        @Override
        public Editor clear() {
            delegate.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return delegate.commit();
        }

        @Override
        public Editor remove(String s) {
            delegate.remove(s);
            return this;
        }

		@Override
		public android.content.SharedPreferences.Editor putStringSet(String key, Set<String> values) {
			throw new RuntimeException("This class does not work with String Sets.");
		}
    }

    public Editor edit() {
        return new Editor();
    }


    @Override
    public Map<String, ?> getAll() {
        throw new UnsupportedOperationException(); // left as an exercise to the reader
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
    	//if these weren't encrypted, then it won't be a string
    	String v;
    	try {
			v = delegate.getString(key, null);
    	} catch (ClassCastException e) {
    		return delegate.getBoolean(key, defValue);
    	}
        return v!=null ? Boolean.parseBoolean(decrypt(v)) : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
    	String v;
    	try {
			v = delegate.getString(key, null);
    	} catch (ClassCastException e) {
    		return delegate.getFloat(key, defValue);
    	}
        return v!=null ? Float.parseFloat(decrypt(v)) : defValue;
    }

    @Override
    public int getInt(String key, int defValue) {
    	String v;
    	try {
			v = delegate.getString(key, null);
    	} catch (ClassCastException e) {
    		return delegate.getInt(key, defValue);
    	}
        return v!=null ? Integer.parseInt(decrypt(v)) : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
    	String v;
    	try {
			v = delegate.getString(key, null);
    	} catch (ClassCastException e) {
    		return delegate.getLong(key, defValue);
    	}
        return v!=null ? Long.parseLong(decrypt(v)) : defValue;
    }

    @Override
    public String getString(String key, String defValue) {
        final String v = delegate.getString(key, null);
        return v != null ? decrypt(v) : defValue;
    }

    @Override
    public boolean contains(String s) {
        return delegate.contains(s);
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        delegate.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        delegate.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

	@Override
	public Set<String> getStringSet(String key, Set<String> defValues) {
		throw new RuntimeException("This class does not work with String Sets.");
	}


    protected String encrypt( String value ) {

        try {
            final byte[] bytes = value!=null ? value.getBytes(UTF8) : new byte[0];
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
            Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
            pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(Settings.Secure.getString(context.getContentResolver(),Settings.System.ANDROID_ID).getBytes(UTF8), 20));
            return new String(Base64Support.encode(pbeCipher.doFinal(bytes), Base64Support.NO_WRAP),UTF8);

        } catch( Exception e ) {
            throw new RuntimeException(e);
        }

    }

    protected String decrypt(String value){
        try {
            final byte[] bytes = value!=null ? Base64Support.decode(value,Base64Support.DEFAULT) : new byte[0];
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
            Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
            pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(Settings.Secure.getString(context.getContentResolver(),Settings.System.ANDROID_ID).getBytes(UTF8), 20));
            return new String(pbeCipher.doFinal(bytes),UTF8);

        } catch( Exception e) {
        	Utils.LogD(this.getClass().getName(), "Warning, could not decrypt the value.  It may be stored in plaintext.  "+e.getMessage());
//            throw new RuntimeException(e);
        	return value;
        }
    }




}