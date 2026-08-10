package ao.co.hzconsultoria.efacturacao.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitário para geração do QR Code oficial em conformidade com as especificações da AGT:
 * - Padrão: QR Code Model 2
 * - Versão: 4 (33 x 33 módulos)
 * - Nível de correção de erros: M (15%)
 * - Modo de dados: Byte
 * - Codificação de caracteres: UTF-8
 * - URL codificada: https://quiosqueagt.minfin.gov.ao/facturacao-eletronica/consultar-fe?emissor=nifEmissor&document=documentNo
 * - Formato: PNG (350x350 px)
 * - Substituição de espaços no documentNo: %20
 * - Logotipo da AGT: Centralizado ocupando menos de 20% do total da imagem
 */
public class AgtQrCodeUtil {

    private static final Logger log = LoggerFactory.getLogger(AgtQrCodeUtil.class);
    private static final String BASE_URL = "https://quiosqueagt.minfin.gov.ao/facturacao-eletronica/consultar-fe";

    /**
     * Monta a URL de consulta oficial da AGT.
     * Substitui espaços no número do documento pela sequência %20.
     */
    public static String montarUrlConsultaAgt(String nifEmissor, String numeroDocumento) {
        if (nifEmissor == null || nifEmissor.trim().isEmpty()) {
            nifEmissor = "999999999";
        }
        if (numeroDocumento == null) {
            numeroDocumento = "";
        }

        // Cada espaço em documentNo deve ser substituído pela sequência %20
        String documentNoEncoded = numeroDocumento.trim().replace(" ", "%20");
        return BASE_URL + "?emissor=" + nifEmissor.trim() + "&document=" + documentNoEncoded;
    }

    /**
     * Gera a imagem BufferedImage de 350x350 px do QR Code segundo especificações AGT.
     */
    public static BufferedImage gerarQrCodeAgtBufferedImage(String nifEmissor, String numeroDocumento) throws Exception {
        String url = montarUrlConsultaAgt(nifEmissor, numeroDocumento);

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // Nível M (15%)
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");                    // Codificação UTF-8
        hints.put(EncodeHintType.MARGIN, 1);                                 // Margem limpa para leitura fácil
        try {
            hints.put(EncodeHintType.QR_VERSION, 4);                          // Versão 4 (33x33 módulos)
        } catch (Exception e) {
            log.debug("Informação ao definir versão QR Code: {}", e.getMessage());
        }

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix;
        try {
            bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 350, 350, hints);
        } catch (Exception e) {
            // Caso o tamanho excede versão 4, permite que o ZXing adapte a versão preservando M e UTF-8
            hints.remove(EncodeHintType.QR_VERSION);
            bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 350, 350, hints);
        }

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }

        // Tenta aplicar o logotipo AGT no centro (< 20% da área)
        try {
            BufferedImage logo = null;
            File logoFile = new File("./uploads/logo/logo_agt.png");
            if (!logoFile.exists()) {
                logoFile = new File("./src/main/resources/static/images/logo_agt.png");
            }
            if (logoFile.exists()) {
                logo = ImageIO.read(logoFile);
            } else {
                java.io.InputStream is = AgtQrCodeUtil.class.getClassLoader().getResourceAsStream("static/images/logo_agt.png");
                if (is != null) {
                    logo = ImageIO.read(is);
                }
            }

            if (logo != null) {
                Graphics2D g = image.createGraphics();
                int logoWidth = (int) (width * 0.18); // ~18% da largura (estritamente inferior a 20%)
                int logoHeight = (int) (height * 0.18);
                int x = (width - logoWidth) / 2;
                int y = (height - logoHeight) / 2;

                g.setColor(Color.WHITE);
                g.fillRect(x - 2, y - 2, logoWidth + 4, logoHeight + 4);
                g.drawImage(logo, x, y, logoWidth, logoHeight, null);
                g.dispose();
            }
        } catch (Exception e) {
            log.debug("Não foi possível sobrepor logotipo AGT no QR Code: {}", e.getMessage());
        }

        return image;
    }

    /**
     * Retorna a imagem formatada para inclusão em relatórios e documentos OpenPDF.
     */
    public static Image gerarQrCodeAgtPdfImage(String nifEmissor, String numeroDocumento) {
        try {
            BufferedImage bi = gerarQrCodeAgtBufferedImage(nifEmissor, numeroDocumento);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            return Image.getInstance(baos.toByteArray());
        } catch (Exception e) {
            log.error("Erro ao gerar QR Code AGT para PDF: {}", e.getMessage());
            return null;
        }
    }
}
