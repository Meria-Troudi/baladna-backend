package tn.esprit.spring.baladna.marketplace.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.marketplace.entity.Commande;
import tn.esprit.spring.baladna.marketplace.entity.Facture;
import tn.esprit.spring.baladna.marketplace.entity.LigneCommande;
import tn.esprit.spring.baladna.marketplace.entity.Product;
import tn.esprit.spring.baladna.marketplace.repository.ProductRepository;
import tn.esprit.spring.baladna.user.entity.User;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfGenerationService {

    private final ProductRepository productRepository;

    // Couleurs
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(59, 130, 246);
    private static final DeviceRgb DARK_COLOR = new DeviceRgb(30, 41, 59);
    private static final DeviceRgb GRAY_COLOR = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb LIGHT_BORDER = new DeviceRgb(226, 232, 240);


    private static final String LOGO_PATH = "C:/Users/User/Desktop/baladna/logo.png";

    public PdfGenerationService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public byte[] generateFacturePdf(Facture facture, Commande commande, User client, List<LigneCommande> lignes) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setMargins(40, 40, 40, 40);

            // ========== HEADER : LOGO + COMPANY NAME ==========
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 3}));
            headerTable.setWidth(UnitValue.createPercentValue(100));
            headerTable.setMarginBottom(30);

            // Cellule Logo
            Cell logoCell = new Cell().setBorder(Border.NO_BORDER);
            try {
                Image logo = new Image(ImageDataFactory.create(LOGO_PATH));
                logo.setWidth(70);
                logo.setHeight(70);
                logoCell.add(logo);
            } catch (Exception e) {
                // Si l'image ne charge pas, afficher un texte
                Paragraph placeholder = new Paragraph("LOGO")
                        .setFontSize(18)
                        .setBold()
                        .setFontColor(PRIMARY_COLOR);
                logoCell.add(placeholder);
            }
            headerTable.addCell(logoCell);

            // Cellule Nom société
            Cell nameCell = new Cell().setBorder(Border.NO_BORDER)
                    .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
            Paragraph companyName = new Paragraph("BALADNA")
                    .setFontSize(30)
                    .setBold()
                    .setFontColor(PRIMARY_COLOR);
            Paragraph tagline = new Paragraph("Your Travel Guide")
                    .setFontSize(14)
                    .setFontColor(GRAY_COLOR)
                    .setMarginTop(5);
            nameCell.add(companyName);
            nameCell.add(tagline);
            headerTable.addCell(nameCell);

            document.add(headerTable);

            // ========== LIGNE SÉPARATRICE ==========
            SolidLine solidLine = new SolidLine(1.5f);
            solidLine.setColor(PRIMARY_COLOR);
            document.add(new LineSeparator(solidLine).setMarginBottom(25));

            // ========== INFO SECTION ==========
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
            infoTable.setWidth(UnitValue.createPercentValue(100));
            infoTable.setMarginBottom(25);

            // Colonne gauche - Infos facture
            Cell leftCell = new Cell().setBorder(Border.NO_BORDER).setPadding(10);
            leftCell.add(new Paragraph("INVOICE DETAILS")
                    .setBold()
                    .setFontColor(DARK_COLOR)
                    .setFontSize(14)
                    .setMarginBottom(10));
            leftCell.add(createInfoRow("Invoice N°", facture.getNumeroFacture()));
            leftCell.add(createInfoRow("Date", facture.getDateEmission().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            leftCell.add(createInfoRow("Order N°", "#" + commande.getId()));

            // Colonne droite - Infos client
            Cell rightCell = new Cell().setBorder(Border.NO_BORDER).setPadding(10);
            rightCell.add(new Paragraph("BILLED TO")
                    .setBold()
                    .setFontColor(DARK_COLOR)
                    .setFontSize(14)
                    .setMarginBottom(10));
            rightCell.add(createInfoRow("Name", client.getFirstName() + " " + client.getLastName()));
            rightCell.add(createInfoRow("Email", client.getEmail()));

            infoTable.addCell(leftCell);
            infoTable.addCell(rightCell);
            document.add(infoTable);

            // ========== LIGNE ==========
            SolidLine line2 = new SolidLine(0.5f);
            line2.setColor(GRAY_COLOR);
            document.add(new LineSeparator(line2).setMarginBottom(20));

            // ========== TABLEAU PRODUITS ==========
            Paragraph tableTitle = new Paragraph("ORDER ITEMS")
                    .setBold()
                    .setFontSize(13)
                    .setFontColor(DARK_COLOR)
                    .setMarginBottom(10);
            document.add(tableTitle);

            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setMarginBottom(20);

            // En-têtes
            table.addHeaderCell(createHeaderCell("Product"));
            table.addHeaderCell(createHeaderCell("Qty"));
            table.addHeaderCell(createHeaderCell("Unit Price"));
            table.addHeaderCell(createHeaderCell("Total"));

            // Lignes
            for (LigneCommande ligne : lignes) {
                String produitNom = "Product #" + ligne.getIdProduit();
                try {
                    Product produit = productRepository.findById(ligne.getIdProduit()).orElse(null);
                    if (produit != null) {
                        produitNom = produit.getNomProduit();
                    }
                } catch (Exception ignored) {}

                table.addCell(createDataCell(produitNom));
                table.addCell(createDataCell(String.valueOf(ligne.getQuantite())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(createDataCell(String.format("%.2f TND", ligne.getPrix())).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(createDataCell(String.format("%.2f TND", ligne.getPrix())).setTextAlignment(TextAlignment.RIGHT));
            }
            document.add(table);

            // ========== LIGNE ==========
            SolidLine line3 = new SolidLine(0.5f);
            line3.setColor(GRAY_COLOR);
            document.add(new LineSeparator(line3).setMarginBottom(20));

            // ========== TOTAUX ==========
            Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
            totalsTable.setWidth(UnitValue.createPercentValue(100));
            totalsTable.setMarginBottom(30);

            Cell emptyCell = new Cell().setBorder(Border.NO_BORDER);
            totalsTable.addCell(emptyCell);

            Cell totalsCell = new Cell().setBorder(Border.NO_BORDER).setPadding(5);
            totalsCell.add(createTotalRow("Subtotal (HT)", facture.getMontantHT()));
            totalsCell.add(createTotalRow("VAT (20%)", facture.getTva()));

            // Total TTC en gras et grand
            Paragraph totalLabel = new Paragraph("TOTAL (TTC)")
                    .setBold()
                    .setFontSize(15)
                    .setFontColor(DARK_COLOR);
            Paragraph totalValue = new Paragraph(String.format("%.2f TND", facture.getMontantTTC()))
                    .setBold()
                    .setFontSize(15)
                    .setFontColor(PRIMARY_COLOR);

            Table totalRow = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
            totalRow.setWidth(UnitValue.createPercentValue(100));
            Cell tLabel = new Cell().setBorder(Border.NO_BORDER).setPaddingTop(10).add(totalLabel);
            Cell tValue = new Cell().setBorder(Border.NO_BORDER).setPaddingTop(10)
                    .add(totalValue.setTextAlignment(TextAlignment.RIGHT));
            totalRow.addCell(tLabel);
            totalRow.addCell(tValue);
            totalsCell.add(totalRow);

            totalsTable.addCell(totalsCell);
            document.add(totalsTable);

            // ========== LIGNE BLEUE ==========
            SolidLine line4 = new SolidLine(1.5f);
            line4.setColor(PRIMARY_COLOR);
            document.add(new LineSeparator(line4).setMarginBottom(20));

            // ========== FOOTER ==========
            Paragraph thankYou = new Paragraph("Thank you for your order!")
                    .setFontSize(16)
                    .setBold()
                    .setFontColor(DARK_COLOR)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(thankYou);

            Paragraph footerText = new Paragraph("For any questions: support@baladna.com")
                    .setFontSize(10)
                    .setFontColor(GRAY_COLOR)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(footerText);

            Paragraph company = new Paragraph("Baladna - Your Travel Guide")
                    .setFontSize(10)
                    .setFontColor(PRIMARY_COLOR)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(company);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }
    }

    private Paragraph createInfoRow(String label, String value) {
        Paragraph p = new Paragraph()
                .setMarginBottom(6);
        p.add(new Paragraph(label + ": ").setFontSize(10).setFontColor(GRAY_COLOR));
        p.add(new Paragraph(value).setFontSize(11).setFontColor(DARK_COLOR).setBold());
        return p;
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setBold()
                        .setFontSize(10)
                        .setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(8)
                .setTextAlignment(TextAlignment.LEFT);
    }

    private Cell createDataCell(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFontSize(10)
                        .setFontColor(DARK_COLOR))
                .setPadding(6)
                .setBorder(new SolidBorder(LIGHT_BORDER, 0.5f));
    }

    private Paragraph createTotalRow(String label, java.math.BigDecimal value) {
        Paragraph p = new Paragraph()
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(5);
        p.add(new Paragraph(label + ": ").setFontSize(10).setFontColor(GRAY_COLOR));
        p.add(new Paragraph(String.format("%.2f TND", value))
                .setFontSize(10)
                .setFontColor(DARK_COLOR));
        return p;
    }
}