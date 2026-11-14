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

	// Pede o CPF e só aceita se for válido (com ou sem pontos e traço)
	public static String lerCpf(Scanner sc) {
		String cpf = lerObrigatorio(sc, "CPF: ").replaceAll("\\D", "");
		while (!cpfValido(cpf)) {
			cpf = lerObrigatorio(sc, "CPF inválido. Digite novamente: ").replaceAll("\\D", "");
		}
		return cpf;
	}

	// Pede o email e só aceita se tiver um formato válido
	public static String lerEmail(Scanner sc, String texto) {
		String email = lerObrigatorio(sc, texto);
		while (!emailValido(email)) {
			email = lerObrigatorio(sc, "Email inválido. " + texto);
		}
		return email;
	}

	// Confere o formato: texto@dominio.com (sem espaços)
	public static boolean emailValido(String email) {
		return email.matches("^[\\w.%+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
	}
	
	// Confere se o CPF tem 11 números e se os dois dígitos verificadores batem
	public static boolean cpfValido(String cpf) {
		// Recusa CPF com tamanho errado ou todos os números iguais (111.111.111-11)
		if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) {
			return false;
		}

		// Soma os 9 primeiros dígitos com os pesos de cada dígito verificador
		int soma1 = 0;
		int soma2 = 0;
		for (int i = 0; i < 9; i++) {
			int digito = cpf.charAt(i) - '0';
			soma1 += digito * (10 - i);
			soma2 += digito * (11 - i);
		}

		// Calcula o primeiro dígito verificador
		int dv1 = soma1 * 10 % 11;
		if (dv1 == 10) {
			dv1 = 0;
		}

		// Calcula o segundo dígito verificador (usa o primeiro na conta)
		soma2 += dv1 * 2;
		int dv2 = soma2 * 10 % 11;
		if (dv2 == 10) {
			dv2 = 0;
		}

		return dv1 == cpf.charAt(9) - '0' && dv2 == cpf.charAt(10) - '0';
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