-- Purchase (vendor) invoice PDF template (XHTML with {{placeholders}}), filled at runtime.
-- Table-based layout + built-in fonts so OpenHTMLtoPDF (flying-saucer) renders it reliably.
-- Ink-light: black on white with thin rules, one light-grey table header band.
INSERT INTO system_setting
    (id, setting_key, setting_value, description, created_at, updated_at)
VALUES
(
    gen_random_uuid(),
    'PURCHASE_INVOICE_TEMPLATE',
    $tmpl$<html>
<head>
<style>
  @page { size: A4; margin: 34px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #1a1a1a; font-size: 12px; }
  .head { width: 100%; border-collapse: collapse; }
  .head td { vertical-align: top; }
  .cname { font-size: 20px; font-weight: bold; letter-spacing: 0.5px; }
  .csub { font-size: 9px; color: #555555; letter-spacing: 1px; margin-top: 2px; }
  .caddr { font-size: 10px; color: #444444; margin-top: 6px; line-height: 1.5; }
  .doc-title { font-size: 22px; font-weight: bold; text-align: right; letter-spacing: 3px; }
  .doc-sub { font-size: 10px; color: #555555; text-align: right; letter-spacing: 3px; margin-top: 2px; }
  .rule { border-bottom: 2px solid #000000; margin: 8px 0 14px 0; }
  .parties { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
  .parties td { vertical-align: top; width: 50%; padding-right: 18px; }
  .eyebrow { font-size: 9px; font-weight: bold; color: #888888; letter-spacing: 1px; margin-bottom: 5px; }
  .pname { font-size: 13px; font-weight: bold; }
  .pline { font-size: 11px; color: #444444; margin-top: 2px; }
  .meta-t { width: 100%; border-collapse: collapse; }
  .meta-t td { padding: 3px 0; font-size: 11px; }
  .meta-k { color: #666666; }
  .meta-v { font-weight: bold; text-align: right; }
  table.items { width: 100%; border-collapse: collapse; margin: 4px 0 12px 0; }
  table.items th { text-align: left; padding: 8px; font-size: 10px; letter-spacing: 0.5px; border-top: 1.5px solid #000000; border-bottom: 1.5px solid #000000; background-color: #f2f2f0; }
  table.items td { padding: 9px 8px; border-bottom: 1px solid #dddddd; font-size: 11px; vertical-align: top; }
  .right { text-align: right; }
  .itype { font-size: 9px; color: #666666; margin-top: 2px; }
  .totals { width: 46%; margin-left: 54%; border-collapse: collapse; margin-bottom: 4px; }
  .totals td { padding: 5px 8px; font-size: 11px; }
  .totals .k { color: #555555; }
  .totals .grand td { border-top: 2px solid #000000; font-size: 15px; font-weight: bold; padding-top: 9px; }
  .pay-h { font-size: 10px; font-weight: bold; letter-spacing: 1px; border-bottom: 1.5px solid #000000; padding-bottom: 5px; margin-top: 14px; }
  .pay-sum { width: 100%; border-collapse: collapse; margin: 10px 0; }
  .pay-sum td { font-size: 11px; padding: 2px 0; }
  table.ph { width: 100%; border-collapse: collapse; }
  table.ph th { text-align: left; font-size: 9px; color: #666666; letter-spacing: 0.5px; padding: 5px 8px; border-bottom: 1px solid #cccccc; }
  table.ph td { font-size: 11px; padding: 7px 8px; border-bottom: 1px solid #eeeeee; }
  .end { width: 100%; border-collapse: collapse; margin-top: 28px; }
  .end td { vertical-align: bottom; }
  .terms { font-size: 10px; color: #555555; width: 62%; line-height: 1.6; }
  .sign { font-size: 10px; color: #555555; text-align: center; }
  .sline { border-top: 1px solid #000000; padding-top: 4px; margin-top: 40px; }
  .foot { margin-top: 18px; border-top: 1px solid #dddddd; padding-top: 8px; text-align: center; color: #999999; font-size: 9px; }
</style>
</head>
<body>
  <table class="head">
    <tr>
      <td>
        <div class="cname">{{companyName}}</div>
        <div class="csub">MANUFACTURING AND TRADING</div>
        <div class="caddr">{{companyAddress}}</div>
        <div class="caddr">{{companyContact}} &#160;&#183;&#160; GSTIN {{companyGstin}}</div>
      </td>
      <td>
        <div class="doc-title">INVOICE</div>
        <div class="doc-sub">PURCHASE</div>
      </td>
    </tr>
  </table>
  <div class="rule"></div>

  <table class="parties">
    <tr>
      <td>
        <div class="eyebrow">VENDOR</div>
        <div class="pname">{{vendorName}}</div>
        <div class="pline">{{vendorAddress}}</div>
        <div class="pline">{{vendorContact}}</div>
        <div class="pline">GSTIN {{vendorGstin}}</div>
      </td>
      <td>
        <table class="meta-t">
          <tr><td class="meta-k">Invoice No.</td><td class="meta-v">{{invoiceNumber}}</td></tr>
          <tr><td class="meta-k">Invoice Date</td><td class="meta-v">{{invoiceDate}}</td></tr>
          <tr><td class="meta-k">Type</td><td class="meta-v">{{invoiceType}}</td></tr>
          <tr><td class="meta-k">Status</td><td class="meta-v">{{paymentStatus}}</td></tr>
        </table>
      </td>
    </tr>
  </table>

  <table class="items">
    <thead>
      <tr>
        <th style="width: 26px;">#</th>
        <th>DESCRIPTION</th>
        <th class="right" style="width: 70px;">QTY</th>
        <th style="width: 58px;">UNIT</th>
        <th class="right" style="width: 82px;">RATE</th>
        <th class="right" style="width: 96px;">AMOUNT</th>
      </tr>
    </thead>
    <tbody>
      {{itemRows}}
    </tbody>
  </table>

  <table class="totals">
    <tr><td class="k">Subtotal</td><td class="right">{{subtotal}}</td></tr>
    <tr><td class="k">GST</td><td class="right">{{gst}}</td></tr>
    <tr><td class="k">Discount</td><td class="right">- {{discount}}</td></tr>
    <tr><td class="k">Other Charges</td><td class="right">{{otherCharge}}</td></tr>
    <tr class="grand"><td>Grand Total</td><td class="right">{{grandTotal}}</td></tr>
  </table>

  <div class="pay-h">PAYMENT DETAILS</div>
  <table class="pay-sum">
    <tr>
      <td>Amount Paid: <b>{{amountPaid}}</b></td>
      <td>Balance Due: <b>{{amountDue}}</b></td>
      <td>Status: <b>{{paymentStatus}}</b></td>
    </tr>
  </table>
  <table class="ph">
    <thead>
      <tr><th style="width: 130px;">DATE</th><th>MODE</th><th class="right" style="width: 110px;">AMOUNT</th></tr>
    </thead>
    <tbody>
      {{paymentRows}}
    </tbody>
  </table>

  <table class="end">
    <tr>
      <td class="terms"><b>Terms.</b> Goods received in full and inspected. Balance payable within 30 days of the invoice date. Please quote the invoice number on all payments.</td>
      <td class="sign"><div>For {{companyName}}</div><div class="sline">Authorised Signatory</div></td>
    </tr>
  </table>

  <div class="foot">Generated on {{generatedOn}}</div>
</body>
</html>$tmpl$,
    'HTML template for the purchase (vendor) invoice PDF (placeholders filled at runtime)',
    NOW(),
    NOW()
)
ON CONFLICT (setting_key) DO NOTHING;
