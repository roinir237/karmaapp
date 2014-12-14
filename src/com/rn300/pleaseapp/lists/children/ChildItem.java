package com.rn300.pleaseapp.lists.children;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.TransitionDrawable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.rn300.pleaseapp.ApiService;
import com.rn300.pleaseapp.R;
import com.rn300.pleaseapp.lists.ListItemInterface;

public class ChildItem implements ListItemInterface{
	private JSONObject childObject;
	private final Context mCtx;
	private Bitmap picBmp = null; 
	
	public static final String ID = "_id";
	public static final String PIC = "pic";
	public static final String NAME = "name";
	public static final String BLOCK = "block";
	public static final String TICKER = "ticker";
	public static final String DEFAULT_TICKER = "";
	public static final String HIGHLIGHT = "highlight";
	
	private class ViewHolder{
		TextView name;
		TextView ticker;
		ImageView pic;
	}
	
	public ChildItem(Context ctx,String string) throws JSONException {
		childObject = new JSONObject(string);
		mCtx = ctx;
		if(childObject.has(PIC)){
			try {
				setBitmap(childObject.getString(PIC));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public ChildItem(Context ctx, JSONObject object){
		childObject = object;
		mCtx = ctx;
		if(childObject.has(PIC)){
			try {
				setBitmap(childObject.getString(PIC));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public Bitmap getProfilePic(){
		if(picBmp == null){
			picBmp = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.ic_contact_picture);
		}
		return picBmp;
	}
	
	public void setBitmap(String image64){
		if(image64.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$")){
			byte[] imageAsBytes = Base64.decode(image64, Base64.NO_WRAP);
			Bitmap image = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
			image = getCroppedBitmap(image);
			this.picBmp = image;
		}else{
			this.picBmp = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.ic_contact_picture);
		}
			
	}
	
	public void setBitmap(Bitmap image){
		image = getCroppedBitmap(image);
		this.picBmp = image;
	}
	
	public void setTicker(Context context,String ticker){
		try {
			childObject.put(TICKER, ticker);
			ApiService.updateChildren(context, childObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	
	public static Bitmap getCroppedBitmap(Bitmap loadedImage) {
		 Bitmap circleBitmap = Bitmap.createBitmap(loadedImage.getWidth(), loadedImage.getHeight(), Bitmap.Config.ARGB_8888);

		    BitmapShader shader = new BitmapShader(loadedImage, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
		    Paint paint = new Paint();
		    paint.setAntiAlias(true);
		    paint.setShader(shader);

		    Canvas c = new Canvas(circleBitmap);
		    c.drawCircle(loadedImage.getWidth() / 2, loadedImage.getHeight() / 2, loadedImage.getWidth() / 2, paint);

		    return circleBitmap;
	}
	
	 public static Bitmap getRoundedRectBitmap(Bitmap bitmap, int pixels)
	    {
	        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
	        Canvas canvas = new Canvas(result);
	 
	        int color = 0xff424242;
	        Paint paint = new Paint();
	        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
	        RectF rectF = new RectF(rect);
	        float roundPx = pixels;
	 
	        paint.setAntiAlias(true);
	        canvas.drawARGB(0, 0, 0, 0);
	        paint.setColor(color);
	        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
	 
	        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	        canvas.drawBitmap(bitmap, rect, rect, paint);
	 
	        return result;
	    }
	 
	public void remove(){
		// TODO 
	}
	
	
	public void block(boolean toBlock){
		try {
			childObject.put(BLOCK, toBlock);
			ApiService.updateChildren(mCtx, childObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isBlocked(){
		if(childObject.has(BLOCK)){
			try {
				return childObject.getBoolean(BLOCK);
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		return false;
	}

	
	public boolean isHidden(){
		if(childObject.has("hidden")){
			try {
				return childObject.getBoolean("hidden");
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		return false;
	}

	
	public String getString(String prop){
		if(childObject.has(prop)){
			try {
				return childObject.getString(prop);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return "";
	}
	
	public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
		ViewHolder holder;
		View view;
		if (convertView == null) {
			final Typeface lightFont = Typeface.createFromAsset(mCtx.getAssets(),"fonts/HelveticaNeue.ttf");
			view = (View) inflater.inflate(R.layout.child_row_view, parent, false);
			TextView nameView = (TextView) view.findViewById(R.id.child_name);
			ImageView image = (ImageView) view.findViewById(R.id.child_image);
			TextView tickerView = (TextView) view.findViewById(R.id.sub_title);
			
			tickerView.setPaintFlags(tickerView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			nameView.setPaintFlags(nameView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			
			nameView.setTypeface(lightFont);
			tickerView.setTypeface(lightFont);
			
			holder = new ViewHolder();
			holder.name = nameView;
			holder.ticker = tickerView;
			holder.pic = image;
			view.setTag(holder);
	    } else {
	    	holder = (ViewHolder) convertView.getTag();
			view = convertView;
		}
	
		try {
			holder.name.setText(childObject.getString("name"));
		} catch (JSONException e) {
			holder.name.setText("Couldn't load chore name");
			e.printStackTrace();
		}
		
		if(picBmp != null){
			holder.pic.setImageBitmap(picBmp);
		}else{
			picBmp = BitmapFactory.decodeResource(mCtx.getResources(),R.drawable.ic_contact_picture);
			holder.pic.setImageBitmap(picBmp);
		}
		
		String ticker = DEFAULT_TICKER;
		if(childObject.has(TICKER)){
			try {
				ticker = childObject.getString(TICKER);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		holder.ticker.setText(ticker);
		
		if(childObject.has(HIGHLIGHT)){
			try {
				childObject.remove(HIGHLIGHT);
				ApiService.updateChildren(mCtx, childObject);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int bottom = view.getPaddingBottom();
			int top = view.getPaddingTop();
			int right = view.getPaddingRight();
			int left = view.getPaddingLeft();
			view.setBackgroundResource( R.drawable.child_highlight_transition );
			TransitionDrawable background = (TransitionDrawable) view.getBackground();
			view.setPadding(left, top, right, bottom);
			
			background.startTransition(mCtx.getResources().getInteger(R.integer.child_highlight_time));
		}
		
		return view;
	}

	@Override 
	public int hashCode() {
		return this.getChildId().hashCode();
	}
	
	@Override 
	public boolean equals(Object obj) {
	        if (obj == null)
	            return false;
	        if (obj == this)
	            return true;
	        if (!(obj instanceof ChildItem))
	            return false;
	       
	        ChildItem c = (ChildItem) obj;
	        if(c.getChildId().equals(this.getChildId()))
	        	return true;
	        else
	        	return false;
	 }

	@Deprecated
	public String getChildId(){
		try {
			return childObject.getString(ID);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public String toString(){
		return childObject.toString();
	}
	
	public JSONObject toJSON(){
		return childObject;
	}

	public static Bitmap getProfilePic(Context context, String id) throws JSONException{
		String image64 = ApiService.getChildProp(context, id, PIC);
		Bitmap image = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_picture);
		if(image64 != null && !image64.equals("")){
			byte[] imageAsBytes = Base64.decode(image64, Base64.NO_WRAP);
			image = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
		}
		
		return image;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}
}
