-- Add a {{paymentRows}} placeholder after the Grand Total row so the invoice PDF can show
-- amount received, any part applied to previous dues, and the remaining balance.
UPDATE system_setting
SET setting_value = REPLACE(
        setting_value,
        '<tr class="grand"><td>Grand Total</td><td class="right">Rs. {{grandTotal}}</td></tr>',
        '<tr class="grand"><td>Grand Total</td><td class="right">Rs. {{grandTotal}}</td></tr>' || chr(10) || '    {{paymentRows}}'
    )
WHERE setting_key = 'SALES_INVOICE_TEMPLATE'
  AND setting_value LIKE '%{{grandTotal}}%'
  AND setting_value NOT LIKE '%{{paymentRows}}%';
