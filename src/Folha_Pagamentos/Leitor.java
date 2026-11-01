package Folha_Pagamentos;

import java.util.Scanner;

public class Leitor {

	// Pede um texto e repete a pergunta até a pessoa digitar algo
	public static String lerObrigatorio(Scanner sc, String texto) {
		System.out.print(texto);
		String valor = sc.nextLine().trim();
		while (valor.isEmpty()) {
			System.out.print("Campo obrigatório. " + texto);
			valor = sc.nextLine().trim();
		}
		return valor;
	}

	// Pede o CNPJ e só aceita se tiver 14 números
	public static String lerCnpj(Scanner sc) {
		String cnpj = lerObrigatorio(sc, "CNPJ da empresa (só números): ");
		while (!cnpj.matches("\\d{14}")) {
			System.out.print("CNPJ deve ter 14 números. Digite novamente: ");
			cnpj = sc.nextLine().trim();
		}
		return cnpj;
	}

	// Pede a senha (mínimo 6 caracteres) e a confirmação até as duas baterem
	public static String lerSenhaComConfirmacao(Scanner sc) {
		while (true) {
			String senha = lerObrigatorio(sc, "Senha (mínimo 6 caracteres): ");
			if (senha.length() < 6) {
				System.out.println("Senha muito curta.");
				continue;
			}
			String confirmacao = lerObrigatorio(sc, "Confirme a senha: ");
			if (senha.equals(confirmacao)) {
				return senha;
			}
			System.out.println("As senhas não conferem. Tente novamente.");
		}
	}
}