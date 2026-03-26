package ao.co.hzconsultoria.efacturacao.service;

public class ConteudoFactura {
	
	public String GetNumeroCredito(String numeroCredito)
	{
	String str ="CRÉDITO N.º "+numeroCredito+"/2022";
	return str;
	}

	public String GetDadosPessoais(String nomeCompleto, String estadoCivil, String numeroBilhete, String nacionalidade,
	  String naturalidade, String residencia,String municipio, String provincia, String numeroTelefone, String email, String valorDivida)
	{
	       String[] provinciaNova = provincia.split("-", 2);
	       
	       System.out.println(" provincia"+ provincia);
	String str = "<p><b>"+nomeCompleto + "</b>,"+ estadoCivil +", Portador do B.I. Nº."+ numeroBilhete + ", de Nacionalidade "+ nacionalidade +", natural de "+naturalidade
	   + ", residente "+ residencia+ ", Município de <b>"+ municipio + "</b>, Província de "+provinciaNova[1] +", usuário do terminal telefónico número "+ numeroTelefone+
	     ", email <b>"+ email+"</b>, adiante designado por <b>SEGUNDA OUTORGANTE;</b><br />";
	return str;
	}

	public String GetConsideracoes(String valorDivida){

	      String str = "CONSIDERANDO QUE:<br />";
	      str += "\n   a) <b>A Primeira Outorgante</b> é uma instituição financeira não bancária e que no âmbito "
	  +  "da sua actividade celebrou com a Segunda Outorgante, um Contrato de Mútuo;<br />";
	      str += "\n   b) <b>A Segunda Outorgante</b> por via do supracitado contrato constitui-se devedora da <b>Primeira Outorgante</b>,"
	  +  " aceitando e confessando a dívida associada ao supracitado contrato e os respectivos juros;<br />";
	      str += "        As Partes estabelecem e ajustam entre si o seguinte: " ;
	return str;
	}
	public String getclausulaPrimeira()
	{
	String str = "1. A presente adenda, visa definir os termos e condições da reestruturação do crédito firmado entre as Partes,\r\n"
	   +" visando um integral e pontual pagamento.\r\n";
	   str +="\n2. O valor da prestação mensal será concedido por transferência bancária directa da Segunda Outorgante para o \r\n"
	   + " Primeiro Outorgante nas seguintes coordenadas bancárias:<br />";
	   str +="\nConta número: <b>088186811 10 001</b><br />";
	   str += "\nIban número: <b>0040-0000-8818-6811-1018-6</b><br />";
	   str +="\nBanco: <b>BAI</b><br />";
	   str +="\nTitular: <b>COOPERATIVA DE CRÉDITO DO FAJE</><br />";
	return str;
	}

	public String getDataReembolso(String dataReembolso)
	{
	String str ="O montante do empréstimo por pagar será reembolsado a partir do dia "+ dataReembolso
	+ ", de acordo com a lista em anexo (plano financeiro reestruturado).";
	return str;
	}
	public String getDataActual(String dataActual)
	{
	String str ="Feita e assinada em Luanda, aos "+ dataActual +";";
	return str;
	}

	public String getDisposicaoComplementar()
	{
	String str ="1. As Partes estabelecem que o contrato principal continua plena e efectivamente válido em tudo quanto não se<br />";
	  str +="\nmanifesta alterado pela presente Adenda.<br />";
	  str +="\n2. Sem prejuízo da apropriação directa definida na cláusula anterior, as Partes estabelecem igualmente que o <br />";
	  str +="\npresente instrumento constitui título bastante para que a <b>Primeira Outorgante</b> torne exigível perante entidade<br />";
	  str +="\njudicial, todas as quantias devidas pela <b>Segunda Outorgante</b>, incluindo respectivos honorários de advogados de<br />";
	  str +="\nque venha a dar lugar em virtude de qualquer incumprimento.<br />" ;
	  str +="\n3. A presente adenda entra em vigor na data da sua assinatura.<br />";

	return str;
	}

}
