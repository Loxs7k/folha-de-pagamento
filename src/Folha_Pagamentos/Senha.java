package Folha_Pagamentos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class Senha {

	// Calcula o hash SHA-256 da senha usando o salt
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
		// Gera um salt aleatório de 16 bytes
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		return Base64.getEncoder().encodeToString(salt) + ":" + hashSenha(senha, salt);
	}

	// Confere se a senha digitada bate com o "salt:hash" guardado no banco
	public static boolean confereSenha(String senha, String guardado) {
		String[] partes = guardado.split(":");
		if (partes.length != 2)
			return false;
		// Refaz o hash da senha digitada com o mesmo salt e compara
		byte[] salt = Base64.getDecoder().decode(partes[0]);
		String hash = hashSenha(senha, salt);
		return MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8), partes[1].getBytes(StandardCharsets.UTF_8));
	}
}
