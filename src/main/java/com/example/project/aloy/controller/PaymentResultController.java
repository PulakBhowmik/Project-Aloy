package com.example.project.aloy.controller;
import com.example.project.aloy.repository.UserRepository;
import com.example.project.aloy.model.User;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Optional;
import com.example.project.aloy.model.Payment;
import com.example.project.aloy.repository.PaymentRepository;
import com.example.project.aloy.repository.ApartmentRepository;
import com.example.project.aloy.model.Apartment;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class PaymentResultController {
    @Autowired
    private UserRepository userRepository;
    @GetMapping("/payment-success")
    public String paymentSuccess(@RequestParam(required = false) String tran_id,
                                 @RequestParam(required = false) String amount,
                                 @RequestParam(required = false) String card_type,
                                 Model model) {
        model.addAttribute("tranId", tran_id);
        model.addAttribute("amount", amount);
        model.addAttribute("cardType", card_type);
        return "payment-success";
    }

    // Serve an HTML page that downloads the PDF receipt and then redirects to home
    @GetMapping(value = "/payment-success/download", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> paymentSuccessDownload(@RequestParam(name = "tran_id", required = false) String tranId) {
        // Clean transaction ID - remove duplicates and commas
        if (tranId != null && tranId.contains(",")) {
            tranId = tranId.split(",")[0].trim();
        }
        String html;
        if (tranId == null || tranId.isBlank()) {
            html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>No Receipt</title></head><body>"
                + "<h2>Receipt not found.</h2>"
                + "<p>You will be redirected to the home page.</p>"
                + "<script>setTimeout(function(){ window.location = '/?refresh=' + Date.now(); }, 1500);</script>"
                + "</body></html>";
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }
        
        // CRITICAL FIX: Mark apartment as booked when user lands on success page
        // This handles the case where SSLCommerz redirects to success_url but doesn't send IPN callback
        System.out.println("[DEBUG] Payment success GET endpoint called with tranId: " + tranId);
        try {
            // Delegate to transactional service to ensure transaction is active
            if (paymentService != null) {
                paymentService.completePaymentAndMarkApartmentBooked(tranId);
            } else {
                // Fallback if for some reason injection failed
                completePaymentAndMarkApartmentBooked(tranId);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to mark apartment as booked: " + e.getMessage());
            e.printStackTrace();
        }
        
            html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Receipt</title></head><body>"
            + "<p>Preparing your receipt... If download does not start, <a id=\"dlLink\" href=\"/receipts/" + tranId + "\">click here</a>.</p>"
            + "<script>\n"
            + "(async function(){\n"
            + "  try{\n"
            + "    const resp = await fetch('/receipts/" + tranId + "');\n"
            + "    if(!resp.ok){\n"
            + "      document.body.innerHTML = '<h2>Receipt not found.</h2><p>You will be redirected to the home page.</p>';\n"
            + "      // Signal frontend to refresh after redirect in case cache interferes\n"
            + "      try{ localStorage.setItem('paymentRefresh', 'true'); }catch(e){};\n"
            + "      setTimeout(function(){ window.location = '/?refresh=' + Date.now(); }, 1500);\n"
            + "      return;\n"
            + "    }\n"
            + "    const blob = await resp.blob();\n"
            + "    const url = window.URL.createObjectURL(blob);\n"
            + "    const a = document.createElement('a'); a.style.display='none'; a.href = url; a.download = 'receipt_" + tranId + ".pdf'; document.body.appendChild(a); a.click();\n"
            + "    window.URL.revokeObjectURL(url);\n"
            + "    document.body.innerHTML = '<h2>✅ Receipt Downloaded Successfully!</h2><p>You will be redirected to the home page in a moment...</p>';\n"
            + "    // Set a localStorage flag so the homepage always performs a fresh fetch and updates UI\n"
            + "    try{ localStorage.setItem('paymentRefresh', 'true'); }catch(e){};\n"
            + "    setTimeout(function(){ window.location = '/?refresh=' + Date.now(); }, 1200);\n"
            + "  }catch(e){ document.body.innerHTML = '<h2>Receipt not found.</h2><p>You will be redirected to the home page.</p>'; try{ localStorage.setItem('paymentRefresh', 'true'); }catch(err){}; setTimeout(function(){ window.location = '/?refresh=' + Date.now(); }, 1500); }\n"
            + "})();\n"
            + "</script></body></html>";
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
    
    // NOTE: transactional booking logic moved to PaymentService to ensure proxy-based @Transactional works
    // Keep this method as a thin wrapper for backwards compatibility (non-transactional)
    public void completePaymentAndMarkApartmentBooked(String tranId) {
        System.out.println("[DEBUG] Controller wrapper called for tranId: " + tranId + ". Delegating to PaymentService.");
        // Delegation happens in the GET handler to ensure proper transactional behavior
    }

    // Handle POST requests to /payment-success/download (for gateway compatibility)
    @PostMapping(value = "/payment-success/download", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> paymentSuccessDownloadPost(@RequestParam(name = "tran_id", required = false) String tranId) {
        return paymentSuccessDownload(tranId);
    }

    // Handle POST callbacks from SSLCommerz (server-to-server or form POST) and return a PDF receipt
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private ApartmentRepository apartmentRepository;
    @Autowired
    private com.example.project.aloy.service.PaymentService paymentService;

    @PostMapping("/payment-success")
    @Transactional
    public ResponseEntity<?> paymentSuccessPost(@RequestParam Map<String, String> allParams) {
    System.out.println("[DEBUG] Payment success POST params: " + allParams);
        // Extract common fields and clean transaction ID
        String tranId = allParams.getOrDefault("tran_id", allParams.getOrDefault("val_id", "N/A"));
        // Clean transaction ID - remove duplicates and commas
        if (tranId.contains(",")) {
            tranId = tranId.split(",")[0].trim();
        }
        System.out.println("[DEBUG] Cleaned transaction ID: " + tranId);
        String amount = allParams.getOrDefault("amount", allParams.getOrDefault("total_amount", "0.00"));
        String cusName = allParams.getOrDefault("cus_name", allParams.getOrDefault("name", "Customer"));

        // Always update or create the payment record by transactionId
    Payment paymentRecord = paymentRepository.findByTransactionId(tranId).orElse(null);
    System.out.println("[DEBUG] Payment record found for transactionId " + tranId + ": " + paymentRecord);
        if (paymentRecord == null) {
            // Create a new payment record if not found
            System.out.println("[DEBUG] Payment record NOT found for transactionId: " + tranId + ". Creating new record.");
            paymentRecord = new Payment();
            paymentRecord.setTransactionId(tranId);
            paymentRecord.setAmount(new java.math.BigDecimal(amount));
            paymentRecord.setPaymentMethod("SSLCommerz");
            paymentRecord.setStatus("COMPLETED");
            paymentRecord.setCreatedAt(java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } else {
            System.out.println("[DEBUG] Payment record FOUND for transactionId: " + tranId + ". Updating status to COMPLETED.");
            paymentRecord.setStatus("COMPLETED");
        }
        // Link apartment and tenant if provided
        String aptIdStr = allParams.getOrDefault("value_a", "");
        String tenantIdStr = allParams.getOrDefault("value_b", "");
        if (!aptIdStr.isEmpty()) {
            try {
                Long aptId = Long.parseLong(aptIdStr);
                Optional<Apartment> lockedAptOpt = apartmentRepository.findByIdForUpdate(aptId);
                if (lockedAptOpt.isPresent()) {
                    Apartment apt = lockedAptOpt.get();
                    if (!apt.isBooked()) {
                        System.out.println("[DEBUG] Marking apartment " + aptId + " as booked and RENTED");
                        apt.setBooked(true);
                        apt.setStatus("RENTED");
                        apartmentRepository.save(apt);
                        System.out.println("[DEBUG] Apartment " + aptId + " successfully marked as booked");
                    } else {
                        System.out.println("[WARNING] Apartment " + aptId + " is already booked!");
                    }
                    paymentRecord.setApartmentId(aptId);
                }
            } catch (NumberFormatException ignored) {}
        }
        if (!tenantIdStr.isEmpty()) {
            try { paymentRecord.setTenantId(Long.parseLong(tenantIdStr)); } catch (NumberFormatException ignored) {}
        }
        Payment savedPayment = paymentRepository.save(paymentRecord);
        System.out.println("[DEBUG] Payment record saved successfully with ID: " + savedPayment.getPaymentId() + ", transactionId: " + savedPayment.getTransactionId());

        try {
            // Find apartment title for receipt if present
            String aptTitle = "Apartment";
            if (!aptIdStr.isEmpty()) {
                try {
                    Long aptId = Long.parseLong(aptIdStr);
                    Optional<Apartment> aopt = apartmentRepository.findById(aptId);
                    if (aopt.isPresent()) aptTitle = aopt.get().getTitle();
                } catch (NumberFormatException ignored) {}
            }

            String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));

            byte[] pdf = generateReceiptPdf(tranId, cusName, amount, allParams, aptTitle, timestamp);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "receipt_" + tranId + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to generate receipt: " + e.getMessage());
        }
    }

    // Retrieve an existing receipt by transaction id (if payment recorded)
    @GetMapping("/receipts/{tranId}")
    public ResponseEntity<?> getReceiptByTranId(@PathVariable String tranId) {
        // Clean transaction ID - remove duplicates and commas
        if (tranId != null && tranId.contains(",")) {
            tranId = tranId.split(",")[0].trim();
        }
    System.out.println("[DEBUG] Receipt request for transactionId: " + tranId);
        Optional<Payment> opt = paymentRepository.findByTransactionId(tranId);
        if (opt.isEmpty()) {
            System.out.println("[ERROR] Payment record NOT FOUND in database for transactionId: " + tranId);
            System.out.println("[ERROR] Please check if payment was saved during initiation or callback.");
            // Return a minimal PDF with error message so download always works
            try {
                byte[] pdf = generateErrorPdf("Receipt not found for transaction: " + tranId);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "receipt_error_" + tranId + ".pdf");
                return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(404).body("Receipt not found for transaction: " + tranId);
            }
        }
        Payment p = opt.get();
        try {
            // Get tenant name if available
            String tenantName = "Customer";
            if (p.getTenantId() != null) {
                Optional<User> tenantOpt = userRepository.findById(p.getTenantId());
                if (tenantOpt.isPresent()) {
                    tenantName = tenantOpt.get().getName();
                }
            }
            
            // Get apartment title if available
            String aptTitle = "Apartment";
            if (p.getApartmentId() != null) {
                Optional<Apartment> aptOpt = apartmentRepository.findById(p.getApartmentId());
                if (aptOpt.isPresent()) {
                    aptTitle = aptOpt.get().getTitle();
                }
            }
            
            // Prepare params with complete info
            Map<String, String> params = Map.of(
                "tran_id", p.getTransactionId(),
                "amount", p.getAmount().toString(),
                "cus_name", tenantName,
                "apartmentId", p.getApartmentId() != null ? p.getApartmentId().toString() : "",
                "tenantId", p.getTenantId() != null ? p.getTenantId().toString() : ""
            );
            
            String timestamp = p.getCreatedAt() != null ? p.getCreatedAt() : ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
            byte[] pdf = generateReceiptPdf(p.getTransactionId(), tenantName, p.getAmount().toString(), params, aptTitle, timestamp);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "receipt_" + tranId + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to generate receipt: " + e.getMessage());
        }
        }
    // Generate a minimal error PDF if receipt is missing
    private byte[] generateErrorPdf(String message) throws Exception {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.LETTER);
        doc.addPage(page);
        PDPageContentStream content = new PDPageContentStream(doc, page);
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 16);
        content.newLineAtOffset(50, 700);
        content.showText("Error: " + message);
        content.endText();
        content.close();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        doc.close();
        return out.toByteArray();
    }

    @GetMapping("/payment-fail")
    public String paymentFail(Model model) {
        model.addAttribute("message", "Payment failed. Please try again.");
        return "payment-fail";
    }

    @GetMapping("/payment-cancel")
    public String paymentCancel(Model model) {
        model.addAttribute("message", "Payment was cancelled.");
        return "payment-cancel";
    }

    private byte[] generateReceiptPdf(String tranId, String name, String amount, Map<String, String> params, String apartmentTitle, String timestamp) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            // Always fetch payment by transactionId for accurate info
            Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(tranId);
            String ownerName = "N/A";
            String ownerEmail = "N/A";
            String tenantName = name != null && !name.isEmpty() ? name : "N/A";
            String tenantPhone = "N/A";
            String tenantEmail = "N/A";
            String apartmentName = apartmentTitle != null && !apartmentTitle.isEmpty() ? apartmentTitle : "N/A";
            String rent = amount != null && !amount.isEmpty() ? amount : "N/A";
            String paymentMethod = "SSLCommerz";
            String paymentStatus = "COMPLETED";
            
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                rent = payment.getAmount() != null ? payment.getAmount().toString() + " BDT" : rent;
                paymentMethod = payment.getPaymentMethod() != null ? payment.getPaymentMethod() : paymentMethod;
                paymentStatus = payment.getStatus() != null ? payment.getStatus() : paymentStatus;
                
                // Get apartment and owner
                if (payment.getApartmentId() != null) {
                    Optional<Apartment> aptOpt = apartmentRepository.findById(payment.getApartmentId());
                    if (aptOpt.isPresent()) {
                        Apartment apt = aptOpt.get();
                        apartmentName = apt.getTitle();
                        if (apt.getOwnerId() != null) {
                            Optional<User> ownerOpt = userRepository.findById(apt.getOwnerId());
                            if (ownerOpt.isPresent()) {
                                User owner = ownerOpt.get();
                                ownerName = owner.getName();
                                ownerEmail = owner.getEmail() != null ? owner.getEmail() : "N/A";
                            }
                        }
                    }
                }
                // Get tenant
                if (payment.getTenantId() != null) {
                    Optional<User> tenantOpt = userRepository.findById(payment.getTenantId());
                    if (tenantOpt.isPresent()) {
                        User tenant = tenantOpt.get();
                        tenantName = tenant.getName();
                        tenantPhone = tenant.getPhoneNumber() != null ? tenant.getPhoneNumber() : "N/A";
                        tenantEmail = tenant.getEmail() != null ? tenant.getEmail() : "N/A";
                    }
                }
            }

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // Header
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                cs.beginText();
                cs.newLineAtOffset(50, 750);
                cs.showText("APARTMENT RENTAL PAYMENT RECEIPT");
                cs.endText();
                
                // Horizontal line
                cs.setLineWidth(1.5f);
                cs.moveTo(50, 735);
                cs.lineTo(550, 735);
                cs.stroke();

                // Transaction Details Section
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.beginText();
                cs.newLineAtOffset(50, 710);
                cs.showText("Transaction Details");
                cs.endText();

                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.beginText();
                cs.newLineAtOffset(50, 690);
                cs.showText("Transaction ID: " + (tranId != null ? tranId : "N/A"));
                cs.newLineAtOffset(0, -16);
                cs.showText("Date/Time: " + (timestamp != null ? timestamp : "N/A"));
                cs.newLineAtOffset(0, -16);
                cs.showText("Payment Method: " + paymentMethod);
                cs.newLineAtOffset(0, -16);
                cs.showText("Payment Status: " + paymentStatus);
                cs.newLineAtOffset(0, -16);
                cs.showText("Amount Paid: " + rent);
                cs.endText();

                // Apartment Details Section
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.beginText();
                cs.newLineAtOffset(50, 610);
                cs.showText("Apartment Details");
                cs.endText();

                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.beginText();
                cs.newLineAtOffset(50, 590);
                cs.showText("Apartment: " + apartmentName);
                cs.newLineAtOffset(0, -16);
                cs.showText("Owner Name: " + ownerName);
                cs.newLineAtOffset(0, -16);
                cs.showText("Owner Email: " + ownerEmail);
                cs.endText();

                // Tenant Details Section
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.beginText();
                cs.newLineAtOffset(50, 530);
                cs.showText("Tenant Details");
                cs.endText();

                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.beginText();
                cs.newLineAtOffset(50, 510);
                cs.showText("Tenant Name: " + tenantName);
                cs.newLineAtOffset(0, -16);
                cs.showText("Tenant Phone: " + tenantPhone);
                cs.newLineAtOffset(0, -16);
                cs.showText("Tenant Email: " + tenantEmail);
                cs.endText();
                
                // Footer
                cs.setLineWidth(0.5f);
                cs.moveTo(50, 100);
                cs.lineTo(550, 100);
                cs.stroke();
                
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
                cs.beginText();
                cs.newLineAtOffset(50, 80);
                cs.showText("Thank you for using our apartment rental service!");
                cs.newLineAtOffset(0, -12);
                cs.showText("For inquiries, please contact support@rentalservice.com");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
