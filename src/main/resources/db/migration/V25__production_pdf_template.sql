-- Widen setting_value so it can hold full HTML templates
ALTER TABLE system_setting ALTER COLUMN setting_value TYPE TEXT;

-- Store the Production PDF template (HTML with {{placeholders}}), fetched by name
INSERT INTO system_setting
    (id, setting_key, setting_value, description, created_at, updated_at)
VALUES
(
    gen_random_uuid(),
    'PRODUCTION_PDF_TEMPLATE',
    $tmpl$<html>
<head>
<style>
  @page { size: A4; margin: 26px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #1E293B; font-size: 12px; }
  .header { text-align: center; border-bottom: 3px solid #2563EB; padding-bottom: 10px; margin-bottom: 16px; }
  .brand { font-size: 22px; font-weight: bold; color: #2563EB; letter-spacing: 1px; }
  .subtitle { font-size: 11px; color: #64748B; letter-spacing: 3px; }
  .report-title { font-size: 15px; font-weight: bold; margin-top: 8px; color: #0F172A; }
  .ref { color: #64748B; font-size: 11px; margin-bottom: 8px; }
  .info-table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
  .info-table td { padding: 5px 6px; vertical-align: top; }
  .info-label { color: #64748B; font-weight: bold; width: 130px; }
  .items { width: 100%; border-collapse: collapse; margin-bottom: 14px; }
  .items th { background-color: #F1F5F9; color: #334155; text-align: left; padding: 7px 8px; border: 1px solid #E2E8F0; font-size: 11px; }
  .items td { padding: 7px 8px; border: 1px solid #E2E8F0; }
  .right { text-align: right; }
  .summary { width: 45%; margin-left: 55%; border-collapse: collapse; margin-top: 4px; }
  .summary td { padding: 5px 8px; border-bottom: 1px solid #E2E8F0; }
  .summary .tot { font-weight: bold; color: #047857; font-size: 13px; }
  .footer { margin-top: 26px; text-align: right; color: #94A3B8; font-size: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
</style>
</head>
<body>
  <div class="header">
    <div class="brand">SKR GARMENT ERP</div>
    <div class="subtitle">PRODUCTION - INVENTORY - SALES MANAGEMENT</div>
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
        <th>Piece Code</th>
        <th class="right">Quantity</th>
        <th class="right">Rate</th>
        <th class="right">Amount</th>
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
</html>$tmpl$,
    'HTML template for production PDF export (placeholders filled at runtime)',
    NOW(),
    NOW()
);
