package Folha_Pagamentos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class Senha {

	public static String hashSenha(String senha, byte[] salt) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			byte[] hash = md.digest(senha.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String criarHash(String senha) {
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		return Base64.getEncoder().encodeToString(salt) + ":" + hashSenha(senha, salt);
	}

	public static boolean confereSenha(String senha, String guardado) {
		String[] partes = guardado.split(":");
		if (partes.length != 2)
			return false;
		byte[] salt = Base64.getDecoder().decode(partes[0]);
		String hash = hashSenha(senha, salt);
		return MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8), partes[1].getBytes(StandardCharsets.UTF_8));
	}
}
