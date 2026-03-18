package ao.co.hzconsultoria.efacturacao;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class EncryptPasswordTest {
    @Test
    public void gerarHashSenha() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String senhaAdmin = "admin123";
        String senhaUser = "user123";
        String hashAdmin = encoder.encode(senhaAdmin);
        String hashUser = encoder.encode(senhaUser);
        System.out.println("Hash admin123: " + hashAdmin);
        System.out.println("Hash user123: " + hashUser);
    }
    public static void main(String[]args) 
    {
    	EncryptPasswordTest test = new EncryptPasswordTest();
    	test.gerarHashSenha();
    	
    }
}
