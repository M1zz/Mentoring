package com.example.mizzz.leeo;

/*
	Date   : 2017.03.28
	Author : Hyunholee
	e-mail : leeo75@cs-cnu.org
*/

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class MD5 {
    String getMD5(String path) throws NoSuchAlgorithmException, FileNotFoundException{
        String result;
        File file = new File(path);
        MessageDigest md = MessageDigest.getInstance("MD5");
        InputStream input = new DigestInputStream(new BufferedInputStream(new FileInputStream(file)),md);
        try{
            while(input.read() != -1 );
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try{
                input.close();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        byte[] hash = md.digest();
        StringBuilder sb = new StringBuilder();
        for(byte b:hash)
            sb.append(String.format("%02X",b));
        result = sb.toString();
        return result;
    }
}
