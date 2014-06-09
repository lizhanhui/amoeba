// BSONEncoder.java

package org.bson;

import org.bson.io.*;
import org.bson.types.*;
import static org.bson.BSON.*;

import java.util.*;
import java.util.regex.*;
import java.util.concurrent.atomic.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.*;
import java.nio.charset.*;

/**
 * this is meant to be pooled or cached
 * there is some per instance memory for string conversion, etc...
 */
public class BSONEncoder {
    
    static final boolean DEBUG = false;

    public BSONEncoder(){

    }

    public byte[] encode( BSONObject o ){
    	 if ( _buf == null ){
    		 BasicOutputBuffer buf = new BasicOutputBuffer();
    		 set( buf );
    	 }
        putObject( o );
        byte[] ret = _buf.toByteArray();
        done();
        return ret;
    }

    public void set( OutputBuffer out ){
        if ( _buf != null )
            throw new IllegalStateException( "in the middle of something" );
        
        _buf = out;
    }
 
    public void done(){
        _buf = null;
    }
   
    /**
     * @return true if object was handled
     */
    protected boolean handleSpecialObjects( String name , BSONObject o ){
        return false;
    }
    
    protected boolean putSpecial( String name , Object o ){
        return false;
    }

    /** Encodes a <code>BSONObject</code>.
     * This is for the higher level api calls
     * @param o the object to encode
     * @return the number of characters in the encoding
     */
    public int putObject( BSONObject o ){
        return putObject( null , o );
    }

    /**
     * this is really for embedded objects
     */
    int putObject( String name , BSONObject o ){
        if ( o == null )
            throw new NullPointerException( "can't save a null object" );

        if ( DEBUG ) System.out.println( "putObject : " + name + " [" + o.getClass() + "]" + " # keys " + o.keySet().size() );
        
        final int start = _buf.getPosition();
        
        byte myType = OBJECT;
        if ( o instanceof List )
            myType = ARRAY;

        if ( handleSpecialObjects( name , o ) )
            return _buf.getPosition() - start;
        
        if ( name != null ){
            _put( myType , name );
        }

        final int sizePos = _buf.getPosition();
        _buf.writeInt( 0 ); // leaving space for this.  set it at the end

        List transientFields = null;
        boolean rewriteID = myType == OBJECT && name == null;
        

        if ( myType == OBJECT ) {
            if ( rewriteID && o.containsField( "_id" ) )
                _putObjectField( "_id" , o.get( "_id" ) );
            
            {
                Object temp = o.get( "_transientFields" );
                if ( temp instanceof List )
                    transientFields = (List)temp;
            }
        }
        

        for ( String s : o.keySet() ){

            if ( rewriteID && s.equals( "_id" ) )
                continue;
            
            if ( transientFields != null && transientFields.contains( s ) )
                continue;
            
            Object val = o.get( s );

            _putObjectField( s , val );

        }
        _buf.write( EOO );
        
        _buf.writeInt( sizePos , _buf.getPosition() - sizePos );
        return _buf.getPosition() - start;
    }

    protected void _putObjectField( String name , Object val ){

        if ( name.equals( "_transientFields" ) )
            return;
        
        if ( DEBUG ) System.out.println( "\t put thing : " + name );
        
        if ( name.equals( "$where") && val instanceof String ){
            _put( CODE , name );
            _putValueString( val.toString() );
            return;
        }
        
        val = BSON.applyEncodingHooks( val );

        if ( val == null )
            putNull(name);
        else if ( val instanceof Date )
            putDate( name , (Date)val );
        else if ( val instanceof Number )
            putNumber(name, (Number)val );
        else if ( val instanceof String )
            putString(name, val.toString() );
        else if ( val instanceof ObjectId )
            putObjectId(name, (ObjectId)val );
        else if ( val instanceof BSONObject )
            putObject(name, (BSONObject)val );
        else if ( val instanceof Boolean )
            putBoolean(name, (Boolean)val );
        else if ( val instanceof Pattern )
            putPattern(name, (Pattern)val );
        else if ( val instanceof Map )
            putMap( name , (Map)val );
        else if ( val instanceof List )
            putList( name , (List)val );
        else if ( val instanceof byte[] )
            putBinary( name , (byte[])val );
        else if ( val instanceof Binary )
            putBinary( name , (Binary)val );
        else if ( val.getClass().isArray() )
            putList( name , Arrays.asList( (Object[])val ) );

        else if (val instanceof Symbol) {
            putSymbol(name, (Symbol) val);
        }
        else if (val instanceof BSONTimestamp) {
            putTimestamp( name , (BSONTimestamp)val );
        }
        else if (val instanceof CodeWScope) {
            putCodeWScope( name , (CodeWScope)val );
        }
        else if ( putSpecial( name , val ) ){
            // no-op
        }
        else {
            throw new IllegalArgumentException( "can't serialize " + val.getClass() );
        }
        
    }

    private void putList( String name , List l ){
        _put( ARRAY , name );
        final int sizePos = _buf.getPosition();
        _buf.writeInt( 0 );
        
        for ( int i=0; i<l.size(); i++ )
            _putObjectField( String.valueOf( i ) , l.get( i ) );

        _buf.write( EOO );
        _buf.writeInt( sizePos , _buf.getPosition() - sizePos );        
    }
    
    private void putMap( String name , Map m ){
        _put( OBJECT , name );
        final int sizePos = _buf.getPosition();
        _buf.writeInt( 0 );
        
        for ( Map.Entry entry : (Set<Map.Entry>)m.entrySet() )
            _putObjectField( entry.getKey().toString() , entry.getValue() );

        _buf.write( EOO );
        _buf.writeInt( sizePos , _buf.getPosition() - sizePos );
    }
    

    protected void putNull( String name ){
        _put( NULL , name );
    }

    protected void putUndefined(String name){
        _put(UNDEFINED, name);
    }

    protected void putTimestamp(String name, BSONTimestamp ts ){
        _put( TIMESTAMP , name );
        _buf.writeInt( ts.getInc() );
        _buf.writeInt( ts.getTime() );
    }
    
    protected void putCodeWScope( String name , CodeWScope code ){
        _put( CODE_W_SCOPE , name );
        int temp = _buf.getPosition();
        _buf.writeInt( 0 );
        _putValueString( code.getCode() );
        putObject( code.getScope() );
        _buf.writeInt( temp , _buf.getPosition() - temp );
    }

    protected void putBoolean( String name , Boolean b ){
        int start = _buf.getPosition();
        _put( BOOLEAN , name );
        _buf.write( b ? (byte)0x1 : (byte)0x0 );
    }

    protected void putDate( String name , Date d ){
        int start = _buf.getPosition();
        _put( DATE , name );
        _buf.writeLong( d.getTime() );
    }

    protected void putNumber( String name , Number n ){
	if ( n instanceof Integer ||
             n instanceof Short ||
             n instanceof Byte ||
             n instanceof AtomicInteger ){
	    _put( NUMBER_INT , name );
	    _buf.writeInt( n.intValue() );
	}
        else if ( n instanceof Long || 
                  n instanceof AtomicLong ) {
            _put( NUMBER_LONG , name );
            _buf.writeLong( n.longValue() );
        }
	else {
	    _put( NUMBER , name );
	    _buf.writeDouble( n.doubleValue() );
	}
    }
    
    protected void putBinary( String name , byte[] data ){
        _put( BINARY , name );
        _buf.writeInt( 4 + data.length );

        _buf.write( B_BINARY );
        _buf.writeInt( data.length );
        int before = _buf.getPosition();
        _buf.write( data );
        int after = _buf.getPosition();
        
        //com.mongodb.util.MyAsserts.assertEquals( after - before , data.length );
    }

    protected void putBinary( String name , Binary val ){
        _put( BINARY , name );
        _buf.writeInt( val.length() );
        _buf.write( val.getType() );
        _buf.write( val.getData() );
    }
    

    protected void putSymbol( String name , Symbol s ){
        _putString(name, s.getSymbol(), SYMBOL);
    }

    protected void putString(String name, String s) {
        _putString(name, s, STRING);
    }

    private void _putString( String name , String s, byte type ){
        _put( type , name );
        _putValueString( s );
    }

    protected void putObjectId( String name , ObjectId oid ){
        _put( OID , name );
        _buf.writeInt( oid._time() );
        _buf.writeInt( oid._machine() );
        _buf.writeInt( oid._inc() );
    }
    
    private void putPattern( String name, Pattern p ) {
        _put( REGEX , name );
        _put( p.pattern() );
        _put( regexFlags( p.flags() ) );
    }


    // ----------------------------------------------
    
    /**
     * Encodes the type and key.
     * 
     */
    protected void _put( byte type , String name ){
        _buf.write( type );
        _put( name );
    }

    protected void _putValueString( String s ){
        int lenPos = _buf.getPosition();
        _buf.writeInt( 0 ); // making space for size
        int strLen = _put( s );
        _buf.writeInt( lenPos , strLen );
    }
    
    void _reset( Buffer b ){
        b.position(0);
        b.limit( b.capacity() );
    }

    /**
     * puts as utf-8 string
     */
    protected int _put( String str ){
        int total = 0;

        final int len = str.length();
        int pos = 0;
        while ( pos < len ){
            _reset( _stringC );
            _reset( _stringB );

            int toEncode = Math.min( _stringC.capacity() - 1, len - pos );
            _stringC.put( str , pos , pos + toEncode );
            _stringC.flip();
            
            CoderResult cr = _encoder.encode( _stringC , _stringB , false );
            
            if ( cr.isMalformed() || cr.isUnmappable() )
                throw new IllegalArgumentException( "malforumed string" );
            
            if ( cr.isOverflow() )
                throw new RuntimeException( "overflor should be impossible" );
            
            if ( cr.isError() )
                throw new RuntimeException( "should never get here" );

            if ( ! cr.isUnderflow() )
                throw new RuntimeException( "this should always be true" );

            total += _stringB.position();
            _buf.write( _stringB.array() , 0 , _stringB.position() );

            pos += toEncode;
        }

        _buf.write( (byte)0 );
        total++;

        return total;
    }

    public void writeInt( int x ){
        _buf.writeInt( x );
    }

    public void writeLong( long x ){
        _buf.writeLong( x );
    }
    
    public void writeCString( String s ){
        _put( s );
    }

    protected OutputBuffer _buf;
    
    private CharBuffer _stringC = CharBuffer.wrap( new char[256 + 1] );
    private ByteBuffer _stringB = ByteBuffer.wrap( new byte[1024 + 1] );
    private CharsetEncoder _encoder = BSON._utf8.newEncoder();

    
    public static void main(String[] args) throws IOException{
    	BSONEncoder encoder = new BSONEncoder();
    	BasicOutputBuffer buffer = new BasicOutputBuffer();
    	
    	//encoder.set(buffer);
    	BSONDecoder decoder = new BSONDecoder();
    	BasicBSONCallback callback = new BasicBSONCallback();
    	BasicBSONObject bson = new BasicBSONObject();
    	
    	CodeWScope scope = new CodeWScope("helloCode",new BasicBSONObject("x",1));
    	bson.put("key", scope);
    	bson.put("key1", "asdfqwerqwer");
    	System.out.println(bson);
    	decoder.decode(encoder.encode(bson), callback);
    	System.out.println(callback.get());
    }
}
