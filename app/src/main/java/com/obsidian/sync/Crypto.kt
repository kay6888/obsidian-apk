package com.obsidian.sync

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

object Crypto {
    
    private const val AES = "AES"
    private const val AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding"
    
    fun generateAESKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance(AES)
        keyGen.init(256, SecureRandom())
        return keyGen.generateKey()
    }
    
    fun encryptAES(data: ByteArray, key: SecretKey): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(AES_CBC_PKCS5)
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encrypted = cipher.doFinal(data)
        return Pair(encrypted, iv)
    }
    
    fun decryptAES(encrypted: ByteArray, key: SecretKey, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_CBC_PKCS5)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        return cipher.doFinal(encrypted)
    }
    
    fun sha256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    fun generateNodeId(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
            .replace("=", "")
            .substring(0, 20)
    }
}
