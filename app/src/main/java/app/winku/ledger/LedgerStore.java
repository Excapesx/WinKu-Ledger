package app.winku.ledger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/** SQLite is the cross-process source of truth; UI saves are three-way field merges. */
public final class LedgerStore extends SQLiteOpenHelper {
    public LedgerStore(Context c) { this(c,"ledger.db"); }
    public LedgerStore(Context c,String name) { super(c,name,null,1); setWriteAheadLoggingEnabled(true); }
    @Override public void onConfigure(SQLiteDatabase db) { try(Cursor c=db.rawQuery("PRAGMA busy_timeout=5000",null)){c.moveToFirst();} }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE meta (k TEXT PRIMARY KEY, value TEXT NOT NULL)");
        db.execSQL("CREATE TABLE txns (id TEXT PRIMARY KEY, json TEXT NOT NULL)");
        db.execSQL("CREATE TABLE inbox (id TEXT PRIMARY KEY, pkg TEXT NOT NULL, nkey TEXT NOT NULL, event_time INTEGER NOT NULL, created INTEGER NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, cents INTEGER NOT NULL, type TEXT NOT NULL, status TEXT NOT NULL, reason TEXT NOT NULL, txn_id TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE INDEX inbox_created ON inbox(created)");
        try { put(db,"ledger",blank()); put(db,"config",new JSONObject("{\"enabled\":true,\"wechat\":true,\"alipay\":true,\"unionpay\":false,\"custom\":\"\",\"retainDays\":30,\"allNotifications\":false}")); }
        catch(JSONException e) { throw new IllegalStateException(e); }
    }
    @Override public void onUpgrade(SQLiteDatabase db,int a,int b) { throw new IllegalStateException("Unsupported database upgrade"); }
    private static JSONObject obj(String s) { try { return new JSONObject(s); } catch(JSONException e) { throw new IllegalArgumentException(e); } }
    private static void put(SQLiteDatabase db,String k,JSONObject o) { ContentValues v=new ContentValues(); v.put("k",k);v.put("value",o.toString());db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE); }
    private static JSONObject get(SQLiteDatabase db,String k) { try(Cursor c=db.rawQuery("SELECT value FROM meta WHERE k=?",new String[]{k})) { return c.moveToFirst()?obj(c.getString(0)):new JSONObject(); } }
    public JSONObject config() { return get(getReadableDatabase(),"config"); }
    public void configure(JSONObject patch) throws JSONException {
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try { JSONObject c=get(db,"config"); for(Iterator<String> it=patch.keys();it.hasNext();) {String k=it.next(); if(Arrays.asList("enabled","wechat","alipay","unionpay","custom","retainDays","allNotifications").contains(k)) c.put(k,patch.get(k));} c.put("retainDays",Math.max(1,Math.min(365,c.optInt("retainDays",30))));put(db,"config",c); prune(db,c); db.setTransactionSuccessful(); } finally {db.endTransaction();}
    }
    public static JSONObject blank() throws JSONException {
        JSONObject d=new JSONObject("{\"v\":1,\"settings\":{\"theme\":\"light\",\"privacy\":false,\"currency\":\"¥\",\"demo\":false},\"accounts\":[],\"categories\":[],\"budgets\":{},\"goals\":[],\"recurring\":[]}");
        JSONArray a=d.getJSONArray("accounts");
        String[][] accounts={{"acc_cash","现金","现金"},{"acc_wx","微信零钱","电子钱包"},{"acc_ali","支付宝","电子钱包"},{"acc_union","云闪付","银行卡"}};
        for(String[] v:accounts) a.put(new JSONObject().put("id",v[0]).put("name",v[1]).put("type",v[2]).put("icon","wallet").put("color","#0E8A4A").put("initial",0).put("archived",false));
        String[][] cats={{"餐饮","expense"},{"购物","expense"},{"交通","expense"},{"居住","expense"},{"日用","expense"},{"娱乐","expense"},{"医疗","expense"},{"其他支出","expense"},{"工资","income"},{"转账收入","income"},{"退款","income"},{"其他收入","income"}};
        for(int i=0;i<cats.length;i++) d.getJSONArray("categories").put(new JSONObject().put("id","cat_"+i).put("name",cats[i][0]).put("type",cats[i][1]).put("icon","tag").put("color","#0E8A4A"));
        return d;
    }
    public JSONObject load() throws JSONException {
        SQLiteDatabase db=getReadableDatabase();db.beginTransactionNonExclusive();
        try {JSONObject d=get(db,"ledger");JSONArray a=new JSONArray();try(Cursor c=db.rawQuery("SELECT json FROM txns ORDER BY rowid DESC",null)){while(c.moveToNext())a.put(obj(c.getString(0)));}d.put("txns",a);db.setTransactionSuccessful();return d;} finally {db.endTransaction();}
    }
    private static Map<String,JSONObject> byId(JSONArray a) throws JSONException {Map<String,JSONObject> m=new LinkedHashMap<>();if(a!=null)for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String id=o.optString("id");if(id.isEmpty())throw new JSONException("记录缺少 ID");m.put(id,o);}return m;}
    private static boolean same(Object a,Object b) { if(a==null||a==JSONObject.NULL)return b==null||b==JSONObject.NULL; if(a instanceof JSONObject && b instanceof JSONObject){JSONObject x=(JSONObject)a,y=(JSONObject)b;if(x.length()!=y.length())return false;for(Iterator<String> it=x.keys();it.hasNext();){String k=it.next();if(!same(x.opt(k),y.opt(k)))return false;}return true;}if(a instanceof JSONArray&&b instanceof JSONArray){JSONArray x=(JSONArray)a,y=(JSONArray)b;if(x.length()!=y.length())return false;for(int i=0;i<x.length();i++)if(!same(x.opt(i),y.opt(i)))return false;return true;}return a.equals(b);}
    private static JSONObject fields(JSONObject live,JSONObject base,JSONObject next) throws JSONException { Set<String> keys=new HashSet<>();base.keys().forEachRemaining(keys::add);next.keys().forEachRemaining(keys::add);for(String k:keys)if(!same(base.opt(k),next.opt(k))){if(next.has(k))live.put(k,next.get(k));else live.remove(k);}return live; }
    public void save(JSONObject base,JSONObject next) throws JSONException {
        for(String k:new String[]{"settings","budgets"})if(!(next.opt(k) instanceof JSONObject))throw new JSONException("备份缺少 "+k);
        for(String k:new String[]{"txns","accounts","categories","goals","recurring"})if(!(next.opt(k) instanceof JSONArray))throw new JSONException("备份缺少 "+k);
        for(String k:new String[]{"txns","accounts","categories","goals","recurring"}){
            JSONArray a=next.getJSONArray(k);
            for(int i=0;i<a.length();i++){
                JSONObject o=a.getJSONObject(i);
                if(!o.optString("id").matches("[a-zA-Z0-9_.:-]{1,160}"))throw new JSONException("无效记录 ID");
                if(o.has("color")&&!o.optString("color").matches("#[0-9a-fA-F]{6}"))throw new JSONException("无效颜色");
                if(k.equals("txns")){
                    if(!o.optString("date").matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")||!o.optString("time").matches("[0-9]{2}:[0-9]{2}"))throw new JSONException("无效日期时间");
                    if(!Arrays.asList("expense","income","transfer").contains(o.optString("type")))throw new JSONException("无效收支方向");
                }
            }
        }
        if(!Arrays.asList("dark","light").contains(next.getJSONObject("settings").optString("theme")))throw new JSONException("无效主题");
        if(next.getJSONObject("settings").optString("currency").matches(".*[<>\"'&].*"))throw new JSONException("无效货币符号");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            Map<String,JSONObject> old=byId(base.optJSONArray("txns")), fresh=byId(next.getJSONArray("txns"));
            for(String id:old.keySet())if(!fresh.containsKey(id)) db.delete("txns","id=?",new String[]{id});
            for(String id:fresh.keySet())if(!same(old.get(id),fresh.get(id))){JSONObject v=fresh.get(id);if(v.optDouble("amount",-1)<0 || !Double.isFinite(v.optDouble("amount",Double.NaN)))throw new JSONException("无效金额");ContentValues cv=new ContentValues();cv.put("id",id);cv.put("json",v.toString());db.insertWithOnConflict("txns",null,cv,SQLiteDatabase.CONFLICT_REPLACE);}
            JSONObject live=get(db,"ledger");
            for(String k:new String[]{"settings","budgets"})live.put(k,fields(live.optJSONObject(k)==null?new JSONObject():live.getJSONObject(k),base.optJSONObject(k)==null?new JSONObject():base.getJSONObject(k),next.getJSONObject(k)));
            for(String k:new String[]{"accounts","categories","goals","recurring"}){
                Map<String,JSONObject> before=byId(base.optJSONArray(k)),after=byId(next.getJSONArray(k)),current=byId(live.optJSONArray(k));
                for(String id:before.keySet())if(!after.containsKey(id))current.remove(id);
                for(String id:after.keySet())if(!same(before.get(id),after.get(id)))current.put(id,after.get(id));
                JSONArray a=new JSONArray();for(JSONObject o:current.values())a.put(o);live.put(k,a);
            }
            put(db,"ledger",live); db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    public boolean accepts(String pkg) {JSONObject c=config();if(!c.optBoolean("enabled",true))return false;return (pkg.equals("com.tencent.mm")&&c.optBoolean("wechat",true))||(pkg.equals("com.eg.android.AlipayGphone")&&c.optBoolean("alipay",true))||(pkg.equals("com.unionpay")&&c.optBoolean("unionpay",false))||Arrays.asList(c.optString("custom").split("[,;\\s]+" )).contains(pkg);}
    public static String hash(String s) {try{byte[] a=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte v:a)b.append(String.format(Locale.ROOT,"%02x",v));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    public void capture(String pkg,String key,long eventTime,String title,String body) throws JSONException {
        if(!accepts(pkg))return;
        TransactionParser.Result r=TransactionParser.parse(pkg,title,body);
        JSONObject config=config();if(!r.candidate&&!config.optBoolean("allNotifications",false))return;
        String id=hash(pkg+"|"+key+"|"+eventTime+"|"+title+"|"+body);
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            ContentValues v=new ContentValues();v.put("id",id);v.put("pkg",pkg);v.put("nkey",key);v.put("event_time",eventTime);v.put("created",System.currentTimeMillis());v.put("title",title);v.put("body",body);v.put("cents",r.cents);v.put("type",r.type);v.put("status",r.automatic?"posted":r.candidate?"pending":"ignored");v.put("reason",r.reason);
            if(db.insertWithOnConflict("inbox",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1 && r.automatic){String tid="auto_"+id;addTransaction(db,tid,pkg,eventTime,title,body,r.cents,r.type);ContentValues u=new ContentValues();u.put("txn_id",tid);db.update("inbox",u,"id=?",new String[]{id});}
            prune(db,config);db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    private void addTransaction(SQLiteDatabase db,String id,String pkg,long time,String title,String body,long cents,String kind) throws JSONException {
        JSONObject meta=get(db,"ledger");String type=kind.equals("refund")?"income":kind;
        String aid=pkg.equals("com.tencent.mm")?"acc_wx":pkg.equals("com.eg.android.AlipayGphone")?"acc_ali":"acc_union";
        boolean exists=false;JSONArray accounts=meta.getJSONArray("accounts");for(int i=0;i<accounts.length();i++)if(accounts.getJSONObject(i).optString("id").equals(aid))exists=true;
        if(!exists){accounts.put(new JSONObject().put("id",aid).put("name",pkg.equals("com.tencent.mm")?"微信零钱":pkg.equals("com.eg.android.AlipayGphone")?"支付宝":"云闪付").put("type","电子钱包").put("initial",0).put("archived",false).put("icon","wallet").put("color","#0E8A4A"));put(db,"ledger",meta);}
        String name=kind.equals("refund")?"退款":type.equals("income")?"其他收入":"其他支出";
        if(type.equals("expense")){String s=title+body;if(s.matches(".*(餐|饭|咖啡|奶茶|外卖|美团|饿了么).*"))name="餐饮";else if(s.matches(".*(地铁|公交|打车|滴滴|车票|出行).*"))name="交通";else if(s.matches(".*(淘宝|京东|拼多多|购物).*"))name="购物";}
        String cid="";JSONArray cats=meta.getJSONArray("categories");for(int i=0;i<cats.length();i++){JSONObject c=cats.getJSONObject(i);if(c.optString("name").equals(name)&&c.optString("type").equals(type)){cid=c.getString("id");break;}}
        if(cid.isEmpty()){cid="native_"+hash(type+name).substring(0,12);cats.put(new JSONObject().put("id",cid).put("name",name).put("type",type).put("icon","tag").put("color","#0E8A4A"));put(db,"ledger",meta);}
        Date date=new Date(time);JSONObject t=new JSONObject().put("id",id).put("type",type).put("amount",cents/100.0).put("categoryId",cid).put("accountId",aid).put("toAccountId","").put("date",new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(date)).put("time",new SimpleDateFormat("HH:mm",Locale.ROOT).format(date)).put("note",title+" · "+body).put("tags",new JSONArray().put("自动记账").put(kind.equals("refund")?"退款":"通知识别")).put("source",pkg).put("minorUnits",cents);
        ContentValues v=new ContentValues();v.put("id",id);v.put("json",t.toString());db.insertWithOnConflict("txns",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }
    private static void prune(SQLiteDatabase db,JSONObject config){db.delete("inbox","created < ?",new String[]{String.valueOf(System.currentTimeMillis()-config.optInt("retainDays",30)*86400000L)});db.execSQL("DELETE FROM inbox WHERE rowid IN (SELECT rowid FROM inbox ORDER BY created DESC LIMIT -1 OFFSET 5000)");}
    public JSONObject inbox(String filter,int limit) throws JSONException {
        SQLiteDatabase db=getReadableDatabase();JSONObject out=new JSONObject();JSONArray a=new JSONArray();String where="pending".equals(filter)?" WHERE status='pending'":"";
        try(Cursor c=db.rawQuery("SELECT id,pkg,event_time,title,body,cents,type,status,reason,txn_id FROM inbox"+where+" ORDER BY created DESC LIMIT ?",new String[]{String.valueOf(Math.max(1,Math.min(limit,500)))})){while(c.moveToNext())a.put(new JSONObject().put("id",c.getString(0)).put("pkg",c.getString(1)).put("time",c.getLong(2)).put("title",c.getString(3)).put("body",c.getString(4)).put("cents",c.getLong(5)).put("type",c.getString(6)).put("status",c.getString(7)).put("reason",c.getString(8)).put("txnId",c.getString(9)));}
        try(Cursor c=db.rawQuery("SELECT status,COUNT(*) FROM inbox GROUP BY status",null)){while(c.moveToNext())out.put(c.getString(0),c.getInt(1));}return out.put("items",a);
    }
    public void resolve(String id,String action) throws JSONException {SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("status",action.equals("discard")?"ignored":"resolved");db.update("inbox",v,"id=? AND status='pending'",new String[]{id});}
    public void clearInbox(){getWritableDatabase().delete("inbox",null,null);}
    public void status(String name,Object value){try{SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{JSONObject s=get(db,"status");s.put(name,value);put(db,"status",s);db.setTransactionSuccessful();}finally{db.endTransaction();}}catch(Exception ignored){}}
    public JSONObject status(){return get(getReadableDatabase(),"status");}
}
