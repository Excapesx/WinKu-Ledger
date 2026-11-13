import app.winku.ledger.TransactionParser;
public class ParserTest {
    static int count;
    static void expect(String pkg,String title,String body,boolean auto,long cents,String type){count++;TransactionParser.Result r=TransactionParser.parse(pkg,title,body);if(r.automatic!=auto||r.cents!=cents||!r.type.equals(type))throw new AssertionError(body+" => "+r.automatic+" "+r.cents+" "+r.type);}
    public static void main(String[] args){String wx="com.tencent.mm",ali="com.eg.android.AlipayGphone";
        expect(wx,"微信支付","收款到账 12.34元",true,1234,"income");
        expect(wx,"微信支付","收款金额￥1,234.56",true,123456,"income");
        expect(wx,"微信支付","支付成功 0.01元",true,1,"expense");
        expect(ali,"支付宝","你已成功付款25.50元",true,2550,"expense");
        expect(ali,"支付宝","退款到账 10元",true,1000,"refund");
        expect(wx,"微信支付","张三向你转账100元，请收款",false,10000,"");
        expect(wx,"朋友","支付成功100元",false,10000,"");
        expect(wx,"微信支付","支付失败100元",false,10000,"");
        expect(ali,"支付宝","支付成功100元，余额20元",false,0,"");
        expect(ali,"支付宝","充值成功100元",false,10000,"transfer");
        expect(ali,"支付宝","优惠券10元，支付成功20元",false,0,"");
        expect(wx,"微信支付","支付成功100元，支付成功100元",true,10000,"expense");
        expect(wx,"微信支付","收到转账",false,0,"");
        expect(ali,"支付宝","预计到账100元",false,10000,"");
        expect(wx,"微信支付","今日收款100元，共3笔收款",false,10000,"");
        expect(ali,"支付宝","转账成功12元",true,1200,"expense");
        expect("com.unknown","交易通知","收款到账10元",false,1000,"");
        expect(ali,"支付宝","收款到账999999999999999999999999元",false,0,"");
        expect(ali,"支付宝","支付成功10元，但未到账",false,1000,"");
        expect(wx,"朋友","你好",false,0,"");
        expect(ali,"支付宝","支付成功1.234元",false,0,"");
        expect(ali,"支付宝","支付成功￥1.234",false,0,"");
        System.out.println("PASS: "+count+" conservative notification parsing cases");
    }
}
