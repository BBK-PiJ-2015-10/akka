import javax.crypto.Cipher;


public class CheckJavaCryptoExtensions {


    public static void main(String[] args) throws Exception {


        System.out.printf("Java %s-bit%n", System.getProperty("sun.arch.data.model"));

        int maxLen = Cipher.getMaxAllowedKeyLength("AES");
        System.out.printf("Max crypto. key length: %d%n", maxLen);
        if (maxLen < Integer.MAX_VALUE) {
            System.out.println("It appears you do NOT have the JCE Unlimited Strength Jurisdiction Policy files installed.");
            System.out.println("Please download these from Oracle's website and unpack to $JAVA_HOME/jre/lib/security.");
        } else {
            System.out.println("It appears you DO have the JCE Unlimited Strength Jurisdiction Policy files installed.");
        }

    }

}
