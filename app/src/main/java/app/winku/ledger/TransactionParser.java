package app.winku.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative offline rules. Never infers a completed payment from an amount alone. */
public final class TransactionParser {
    private static final Pattern MONEY = Pattern.compile("(?<![0-9.,])(?:[¥￥]\\s*([0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)(?![0-9.,])|([0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)\\s*元)");
    public static final class Result {
        public final boolean candidate, automatic;
        public final long cents;
        public final String type, reason;
        Result(boolean c, boolean a, long n, String t, String r) { candidate=c; automatic=a; cents=n; type=t; reason=r; }
    }
    private static boolean has(String s, String regex) { return Pattern.compile(regex).matcher(s).find(); }
    public static Result parse(String pkg, String title, String body) {
        String text = (title + " " + body).replace('\u00a0',' ');
        boolean candidate = has(text,"支付|付款|收款|到账|转账|转入|转出|退款|提现|充值|扣款|交易|红包|人民币|[¥￥]|[0-9]元");
        if (!candidate) return new Result(false,false,0,"","非交易通知");
        Set<Long> amounts = new LinkedHashSet<>();
        Matcher m = MONEY.matcher(text);
        try {
            while(m.find()) {
                String v = m.group(1) != null ? m.group(1) : m.group(2);
                long cents = new BigDecimal(v.replace(",", "")).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
                if (cents > 0 && cents <= 100000000000L) amounts.add(cents);
            }
        } catch (ArithmeticException e) { return new Result(true,false,0,"","金额超出支持范围"); }
        long cents = amounts.size()==1 ? amounts.iterator().next() : 0;
        if (has(text,"待收款|待确认|请收款|请确认|向你转账|给你转账|待领取|待支付|待付款|待入账|处理中|申请中|预计到账|将到账|尚未到账|未到账|未支付|未付款|失败|关闭|取消|退回|撤销|过期|付款请求|催收"))
            return new Result(true,false,cents,"","交易未完成或状态不明确");
        if (has(text,"优惠|立减|优惠券|最高可得|赢取|领取红包|红包提醒|红包封面|余额不足|账单日|还款提醒|待还|已花费|累计|汇总|共[收支]|今日收款|昨日收款|笔交易|笔收款"))
            return new Result(true,false,cents,"","营销、提醒或汇总，不自动入账");
        if (cents == 0) return new Result(true,false,0,"", amounts.size()>1 ? "包含多个金额，无法确定交易金额" : "通知没有可用的明确金额");
        if ("com.tencent.mm".equals(pkg) && !has(title,"微信支付|微信收款助手|收款通知|微信转账|微信红包"))
            return new Result(true,false,cents,"","普通聊天或来源不明确，避免把聊天内容记成账");
        if (!"com.tencent.mm".equals(pkg) && !"com.eg.android.AlipayGphone".equals(pkg) && !"com.unionpay".equals(pkg))
            return new Result(true,false,cents,"","自定义平台先保留通知，不使用通用规则自动入账");
        if (has(text,"退款成功|退款到账|退款已到账|已退款|收到退款")) return new Result(true,true,cents,"refund","退款到账");
        if (has(text,"提现成功|充值成功|转入零钱|转入余额|转入余额宝|转出余额宝"))
            return new Result(true,false,cents,"transfer","账户内部转移，需要确定转出与转入账户");
        boolean income = has(text,"收款到账|收款成功|收款金额|成功收款|收到.*(?:转账|付款|红包)|转账已到账|转账到账|已收款|收钱到账|收入到账");
        boolean expense = has(text,"支付成功|付款成功|成功付款|成功支付|已付款|已支付|支出金额|支出[：:]|扣款成功|成功扣款|转账成功|已向.*(?:转账|付款)|你向.*(?:转账|付款)");
        if (income && expense) return new Result(true,false,cents,"","收支方向冲突");
        if(income) return new Result(true,true,cents,"income","明确收款到账");
        if(expense) return new Result(true,true,cents,"expense","明确付款成功");
        return new Result(true,false,cents,"","未匹配到明确的交易完成规则");
    }
}
