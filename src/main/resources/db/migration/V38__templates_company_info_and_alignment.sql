-- Two fixes applied to the stored PDF templates:
--   1. Header company block is now read from settings ({{companyName}}/{{companyAddress}}/
--      {{companyContact}}/{{companyGstin}}) instead of the hard-coded "SKR GARMENT ERP".
--   2. Item tables use table-layout:fixed with explicit column widths so the header cells
--      and the body cells line up perfectly in the PDF renderer.

-- ---------- Sales invoice ----------
UPDATE system_setting SET setting_value = $tmpl$<html>
<head>
<style>
  @page { size: A4; margin: 32px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #1E293B; font-size: 12px; }
  .head { width: 100%; border-collapse: collapse; margin-bottom: 8px; }
  .head td { vertical-align: top; }
  .brand { font-size: 24px; font-weight: bold; color: #2563EB; letter-spacing: 1px; }
  .inv-title { font-size: 26px; font-weight: bold; color: #0F172A; text-align: right; }
  .inv-no { font-size: 12px; color: #64748B; text-align: right; }
  .divider { border-bottom: 3px solid #2563EB; margin: 6px 0 16px 0; }
  .meta { width: 100%; border-collapse: collapse; margin-bottom: 18px; }
  .meta td { vertical-align: top; padding: 0 6px; width: 50%; }
  .label { font-size: 10px; font-weight: bold; color: #94A3B8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 3px; }
  .value { font-size: 13px; font-weight: bold; color: #0F172A; }
  .muted { color: #64748B; font-size: 11px; margin-top: 2px; }
  .badge { display: inline-block; padding: 3px 10px; border-radius: 10px; font-size: 10px; font-weight: bold; }
  .badge-paid { background-color: #DCFCE7; color: #166534; }
  .badge-other { background-color: #FEF3C7; color: #92400E; }
  table.items { width: 100%; border-collapse: collapse; margin-bottom: 14px; table-layout: fixed; }
  table.items th { background-color: #0F172A; color: #ffffff; text-align: left; padding: 8px 10px; font-size: 11px; }
  table.items td { padding: 8px 10px; border-bottom: 1px solid #E2E8F0; word-wrap: break-word; }
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
        <div class="brand">{{companyName}}</div>
        <div class="muted">{{companyAddress}}</div>
        <div class="muted">{{companyContact}} &#160;&#183;&#160; GSTIN {{companyGstin}}</div>
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
        <th class="right" style="width: 62px;">Qty</th>
        <th class="right" style="width: 92px;">Price</th>
        <th class="right" style="width: 92px;">Discount</th>
        <th class="right" style="width: 104px;">Amount</th>
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
</html>$tmpl$
WHERE setting_key = 'SALES_INVOICE_TEMPLATE';

-- ---------- Production report ----------
UPDATE system_setting SET setting_value = $tmpl$<html>
<head>
<style>
  @page { size: A4; margin: 26px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #1E293B; font-size: 12px; }
  .header { text-align: center; border-bottom: 3px solid #2563EB; padding-bottom: 10px; margin-bottom: 16px; }
  .brand { font-size: 22px; font-weight: bold; color: #2563EB; letter-spacing: 1px; }
  .subtitle { font-size: 11px; color: #64748B; margin-top: 2px; }
  .report-title { font-size: 15px; font-weight: bold; margin-top: 8px; color: #0F172A; }
  .ref { color: #64748B; font-size: 11px; margin-bottom: 8px; }
  .info-table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
  .info-table td { padding: 5px 6px; vertical-align: top; }
  .info-label { color: #64748B; font-weight: bold; width: 130px; }
  .items { width: 100%; border-collapse: collapse; margin-bottom: 14px; table-layout: fixed; }
  .items th { background-color: #F1F5F9; color: #334155; text-align: left; padding: 7px 8px; border: 1px solid #E2E8F0; font-size: 11px; }
  .items td { padding: 7px 8px; border: 1px solid #E2E8F0; word-wrap: break-word; }
  .right { text-align: right; }
  .summary { width: 45%; margin-left: 55%; border-collapse: collapse; margin-top: 4px; }
  .summary td { padding: 5px 8px; border-bottom: 1px solid #E2E8F0; }
  .summary .tot { font-weight: bold; color: #047857; font-size: 13px; }
  .footer { margin-top: 26px; text-align: right; color: #94A3B8; font-size: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
</style>
</head>
<body>
  <div class="header">
    <div class="brand">{{companyName}}</div>
    <div class="subtitle">{{companyAddress}}</div>
    <div class="subtitle">{{companyContact}} &#160;&#183;&#160; GSTIN {{companyGstin}}</div>
    <div class="report-title">Production Report</div>
  </div>
  <div class="ref">Ref ID: {{refId}}</div>
  <table class="info-table">
    <tr><td class="info-label">Production Date</td><td>{{productionDate}}</td></tr>
    <tr><td class="info-label">Employee</td><td>{{employeeName}}</td></tr>
    <tr><td class="info-label">Remarks</td><td>{{remarks}}</td></tr>
  </table>
  <table class="items">
    <thead>
      <tr>
        <th>Product</th>
        <th style="width: 110px;">Piece Code</th>
        <th class="right" style="width: 80px;">Quantity</th>
        <th class="right" style="width: 90px;">Rate</th>
        <th class="right" style="width: 100px;">Amount</th>
      </tr>
    </thead>
    <tbody>
      {{itemRows}}
    </tbody>
  </table>
  <table class="summary">
    <tr><td>Total Products</td><td class="right">{{productCount}}</td></tr>
    <tr><td>Total Quantity</td><td class="right">{{totalQuantity}} Pcs</td></tr>
    <tr><td class="tot">Total Amount</td><td class="right tot">Rs. {{totalAmount}}</td></tr>
  </table>
  <div class="footer">Generated on {{generatedOn}}</div>
</body>
</html>$tmpl$
WHERE setting_key = 'PRODUCTION_PDF_TEMPLATE';
