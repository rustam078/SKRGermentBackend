-- Store the Sales Invoice PDF template (HTML with {{placeholders}}), fetched by name
INSERT INTO system_setting
    (id, setting_key, setting_value, description, created_at, updated_at)
VALUES
(
    gen_random_uuid(),
    'SALES_INVOICE_TEMPLATE',
    $tmpl$<html>
<head>
<style>
  @page { size: A4; margin: 32px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #1E293B; font-size: 12px; }
  .head { width: 100%; border-collapse: collapse; margin-bottom: 8px; }
  .brand { font-size: 24px; font-weight: bold; color: #2563EB; letter-spacing: 1px; }
  .brand-sub { font-size: 10px; color: #64748B; letter-spacing: 2px; }
  .inv-title { font-size: 26px; font-weight: bold; color: #0F172A; text-align: right; }
  .inv-no { font-size: 12px; color: #64748B; text-align: right; }
  .divider { border-bottom: 3px solid #2563EB; margin: 6px 0 16px 0; }
  .meta { width: 100%; border-collapse: collapse; margin-bottom: 18px; }
  .meta td { vertical-align: top; padding: 0 6px; width: 50%; }
  .label { font-size: 10px; font-weight: bold; color: #94A3B8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 3px; }
  .value { font-size: 13px; font-weight: bold; color: #0F172A; }
  .muted { color: #64748B; font-size: 11px; }
  .badge { display: inline-block; padding: 3px 10px; border-radius: 10px; font-size: 10px; font-weight: bold; }
  .badge-paid { background-color: #DCFCE7; color: #166534; }
  .badge-other { background-color: #FEF3C7; color: #92400E; }
  table.items { width: 100%; border-collapse: collapse; margin-bottom: 14px; }
  table.items th { background-color: #0F172A; color: #ffffff; text-align: left; padding: 8px 10px; font-size: 11px; }
  table.items td { padding: 8px 10px; border-bottom: 1px solid #E2E8F0; }
  .right { text-align: right; }
  .totals { width: 46%; margin-left: 54%; border-collapse: collapse; }
  .totals td { padding: 5px 10px; }
  .totals .grand td { border-top: 2px solid #0F172A; font-size: 15px; font-weight: bold; color: #2563EB; }
  .footer { margin-top: 30px; border-top: 1px solid #E2E8F0; padding-top: 10px; color: #94A3B8; font-size: 10px; text-align: center; }
</style>
</head>
<body>
  <table class="head">
    <tr>
      <td>
        <div class="brand">SKR GARMENT ERP</div>
        <div class="brand-sub">PRODUCTION - INVENTORY - SALES</div>
      </td>
      <td>
        <div class="inv-title">INVOICE</div>
        <div class="inv-no">{{invoiceNumber}}</div>
      </td>
    </tr>
  </table>
  <div class="divider"></div>

  <table class="meta">
    <tr>
      <td>
        <div class="label">Billed To</div>
        <div class="value">{{customerName}}</div>
        <div class="muted">{{customerMobile}}</div>
        <div class="muted">{{customerEmail}}</div>
      </td>
      <td>
        <div class="label">Invoice Date</div>
        <div class="value">{{date}}</div>
        <div class="label" style="margin-top:8px;">Payment</div>
        <div class="value">{{paymentMode}} <span class="badge {{statusClass}}">{{paymentStatus}}</span></div>
        <div class="muted">{{paymentProvider}}</div>
      </td>
    </tr>
  </table>

  <table class="items">
    <thead>
      <tr>
        <th>Product</th>
        <th class="right">Qty</th>
        <th class="right">Price</th>
        <th class="right">Discount</th>
        <th class="right">Amount</th>
      </tr>
    </thead>
    <tbody>
      {{itemRows}}
    </tbody>
  </table>

  <table class="totals">
    <tr><td>Subtotal</td><td class="right">Rs. {{subtotal}}</td></tr>
    <tr><td>Discount</td><td class="right">- Rs. {{discount}}</td></tr>
    <tr><td>Tax</td><td class="right">Rs. {{tax}}</td></tr>
    <tr class="grand"><td>Grand Total</td><td class="right">Rs. {{grandTotal}}</td></tr>
  </table>

  <div class="footer">Thank you for your business. Generated on {{generatedOn}}.</div>
</body>
</html>$tmpl$,
    'HTML template for sales invoice PDF (placeholders filled at runtime)',
    NOW(),
    NOW()
);
