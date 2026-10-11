package Folha_Pagamentos;

public class descontos {

	double Inss;
    double Irrf;
    double Fgts;
    double irpf;
    public double TotalDescontos;
    
  //===================== CALCULO INSS (PROGRESSIVO) =====================
    public double InssDesconto(double totalProventos) {
        double[] teto = {1621.00, 2902.84, 4354.27, 8475.55};
        double[] taxa = {0.075, 0.09, 0.12, 0.14};
        double inss = 0;
        double anterior = 0;

        for (int i = 0; i < teto.length; i++) {
            double faixa = Math.min(totalProventos, teto[i]) - anterior;
            if (faixa <= 0) {
                break;
            }
            inss += faixa * taxa[i];
            anterior = teto[i];
        }

        Inss = Math.round((inss + 0.0000001) * 100.0) / 100.0;
        return Inss;
    }
    
  //===================== CALCULO IRRF =====================
    public double IrrfDesconto(double totalProventos, double Inss) {
       
        double baseIrrf = totalProventos - Math.max(Inss, 607.20);
        double imposto;

        if (baseIrrf <= 2428.80) {
            imposto = 0;
        } else if (baseIrrf <= 2826.65) {
            imposto = (baseIrrf * 0.075) - 182.16;
        } else if (baseIrrf <= 3751.05) {
            imposto = (baseIrrf * 0.15) - 394.16;
        } else if (baseIrrf <= 4664.68) {
            imposto = (baseIrrf * 0.225) - 675.49;
        } else {
            imposto = (baseIrrf * 0.275) - 908.73;
        }

        if (totalProventos <= 5000.00) {
            imposto = 0;
        } else if (totalProventos <= 7350.00) {
            double reducao = Math.max(0, 978.62 - (0.133145 * totalProventos));
            imposto = imposto - Math.min(reducao, imposto);
        }

        Irrf = Math.round((imposto + 0.0000001) * 100.0) / 100.0;
        return Irrf;
    }
    
    //===================== FGTS DESCONTO =====================
    public double FgtsDesconto(double totalProventos) {

        Fgts = totalProventos * 0.08;

        return Fgts;
    }
    
    //===================== SOMA =====================
    public double calcularTotalDescontos() {
        TotalDescontos = Inss + Irrf;
        return TotalDescontos;
    }
    
    
    
}
