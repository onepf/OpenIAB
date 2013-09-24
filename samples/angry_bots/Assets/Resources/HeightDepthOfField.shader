 Shader "Hidden/HeightDepthOfField" {
	Properties {
		_MainTex ("Base", 2D) = "" {}
		_TapLowBackground ("TapLowBackground", 2D) = "" {}
		_TapLowForeground ("TapLowForeground", 2D) = "" {}
		_TapMedium ("TapMedium", 2D) = "" {}
	}

	CGINCLUDE
	
	#include "UnityCG.cginc"
	
	struct v2fDofApply {
		float4 pos : POSITION;
		float2 uv : TEXCOORD0;
	};
			
	sampler2D _MainTex;
	sampler2D _CameraDepthTexture;
	sampler2D _TapLowBackground;	
	sampler2D _TapLowForeground;
	sampler2D _TapMedium;
	
	half4 _ObjectFocusParameter;
	half4x4 _FrustumCornersWS;
	half4 _CameraWS;			
		
	uniform float4 _MainTex_TexelSize;		
		
	v2fDofApply vertDofApply( appdata_img v ) {
		v2fDofApply o;
		o.pos = mul(UNITY_MATRIX_MVP, v.vertex);
		o.uv.xy = v.texcoord.xy;
				
		return o;
	} 
	
	float _ForegroundBlurExtrude;
			
	struct v2fDown {
		float4 pos : POSITION;
		float2 uv0 : TEXCOORD0;
		float2 uv[2] : TEXCOORD1;
	};
	
	uniform float2 _InvRenderTargetSize;
	
	v2fDown vertDownsample(appdata_img v) {
		v2fDown o;
		o.pos = mul(UNITY_MATRIX_MVP, v.vertex);
		
		o.uv0.xy = v.texcoord.xy;
		o.uv[0].xy = v.texcoord.xy + float2( -1.0, -1.0 ) * _InvRenderTargetSize;
		o.uv[1].xy = v.texcoord.xy + float2( +1.0, -1.0 ) * _InvRenderTargetSize;		
		
		return o;
	} 
	
	struct v2f_ws {
		float4 pos : POSITION;
		float2 uv : TEXCOORD0;
		float3 interpolatedRay : TEXCOORD1;
	};

	
	v2f_ws vertWithWsReconstruct( appdata_img v )
	{
		v2f_ws o; 
				
		half3 tmpVertex = v.vertex.xyz;
				
		v.vertex.z = 0.1;
		o.pos = mul(UNITY_MATRIX_MVP, v.vertex);
		o.uv = v.texcoord;
		
		o.interpolatedRay = _FrustumCornersWS[(int)tmpVertex.z].xyz;
		
		return o;
	}	
	
	// @NOTE: this actually fucks with the clean mask,
	// mixing COC weightes of foreground and background, 
	// the result however is not very perceivable
	
	half4 fragDownsample(v2fDown i) : COLOR {
		float2 rowOfs[4];   
		
  		rowOfs[0] = 0;  
  		rowOfs[1] = half2(0.0, _InvRenderTargetSize.y);  
  		rowOfs[2] = half2(0.0, _InvRenderTargetSize.y) * 2;  
  		rowOfs[3] = half2(0.0, _InvRenderTargetSize.y) * 3; 
  		
  		half4 color = (tex2D(_MainTex, i.uv0.xy)); 	
			
		color += (tex2D(_MainTex, i.uv[0].xy + rowOfs[0]));  
		color += (tex2D(_MainTex, i.uv[1].xy + rowOfs[0]));  
		color += (tex2D(_MainTex, i.uv[0].xy + rowOfs[2]));  
		color += (tex2D(_MainTex, i.uv[1].xy + rowOfs[2]));  
		
		color /= 5;
  		
		return color;
	}
	
	half4 fragDofApplyBg (v2fDofApply i) : COLOR {		
		half4 finalColor = half4 (0.0, 0.0, 0.0, 1.0);
		half4 tapHigh = tex2D (_MainTex, i.uv.xy);
		half4 tapLow = tex2D (_TapLowBackground, i.uv.xy);
		
		finalColor = lerp (tapHigh, tapLow, saturate(tapHigh.a * 2.0f));

		return finalColor; 
	}	
	
	half4 fragDofApplyBgDebug (v2fDofApply i) : COLOR {		
		half4 tapHigh = tex2D (_MainTex, i.uv.xy); 
		half4 tapLow = tex2D (_TapLowBackground, i.uv.xy);
		
		// @NOTE: need to simulate the low rez pass mixing here
		half4 tapMedium = tex2D (_TapMedium, i.uv.xy);
		tapMedium.rgb = (tapMedium.rgb + half3 (1, 1, 0)) * 0.5;	
		tapLow.rgb = (tapLow.rgb + half3 (0, 1, 0)) * 0.5;
		
		tapLow = lerp (tapMedium, tapLow, saturate (tapLow.a * tapLow.a));		
		tapLow = tapLow * 0.5 + tex2D (_TapLowBackground, i.uv.xy) * 0.5;

		return lerp (tapHigh, tapLow, tapHigh.a);
	}		
	
	half4 fragDofApplyFg (v2fDofApply i) : COLOR {		
		half4 fgBlur = tex2D(_TapLowForeground, i.uv.xy);	
		//half4 fgBlur = tex2D(_TapLowForeground, i.uv.xy);		
		half4 fgColor = tex2D(_MainTex,i.uv.xy);
				
		// many different ways to combine the blurred coc and the high resolution coc
		// we are using the 2*blurredCoc-highResCoc from CallOfDuty, 'cause it seems to
		// give most satisfying results for most cases
		
		//fgBlur.a = saturate(fgBlur.a*_ForegroundBlurWeight+saturate(fgColor.a-fgBlur.a));
		fgBlur.a = max (fgColor.a, (2.0 * fgBlur.a - fgColor.a)) * _ForegroundBlurExtrude;
		
		return lerp (fgColor, fgBlur, saturate(fgBlur.a));
	}	
	
	half4 fragDofApplyFgDebug (v2fDofApply i) : COLOR {
		half4 fgBlur = tex2D(_TapLowForeground, i.uv.xy);			
		half4 fgColor = tex2D(_MainTex,i.uv.xy);
		
		fgBlur.a = max (fgColor.a, (2.0*fgBlur.a-fgColor.a)) * _ForegroundBlurExtrude;
		
		half4 tapMedium = half4 (1, 1, 0, fgBlur.a);	
		tapMedium.rgb = 0.5 * (tapMedium.rgb + fgColor.rgb);
		
		fgBlur.rgb = 0.5 * (fgBlur.rgb + half3(0,1,0));
		fgBlur.rgb = lerp (tapMedium.rgb, fgBlur.rgb, saturate (fgBlur.a * fgBlur.a));
		
		return lerp ( fgColor, fgBlur, saturate(fgBlur.a));
	}	
		
	half4 fragCocBg (v2f_ws i) : COLOR {		
		float dpth = UNITY_SAMPLE_DEPTH(tex2D (_CameraDepthTexture, i.uv));
		dpth = Linear01Depth (dpth);
		float3 wsDir = (_CameraWS.xyz  + dpth * i.interpolatedRay);
		half coc = saturate(_ObjectFocusParameter.y * (-wsDir.y + _ObjectFocusParameter.x - _ObjectFocusParameter.w));
		return coc;
	}
	
	half4 fragCocAndColorFg (v2f_ws i) : COLOR {		
		half4 color = tex2D (_MainTex, i.uv.xy);
		float dpth = UNITY_SAMPLE_DEPTH(tex2D (_CameraDepthTexture, i.uv));
		dpth = Linear01Depth (dpth);
		float3 wsDir = (_CameraWS.xyz  + dpth * i.interpolatedRay);
		
		half coc = saturate(_ObjectFocusParameter.y * ((-_ObjectFocusParameter.x - _ObjectFocusParameter.w) + wsDir.y));
		color.a = coc;
		
		return color;	
	}		
 
	ENDCG
	
Subshader {
 
 // pass 0
 
 Pass {
	  ZTest Always Cull Off ZWrite Off
	  Fog { Mode off }      

      CGPROGRAM
      #pragma fragmentoption ARB_precision_hint_fastest
      #pragma vertex vertDofApply
      #pragma fragment fragDofApplyBg
      
      ENDCG
  	}

 // pass 1
 
 Pass {
	  ZTest Always Cull Off ZWrite Off
	  Fog { Mode off }      

      CGPROGRAM
      #pragma fragmentoption ARB_precision_hint_fastest
      #pragma vertex vertDofApply
      #pragma fragment fragDofApplyFgDebug

      ENDCG
  	}

 // pass 2

 Pass {
	  ZTest Always Cull Off ZWrite Off
	  Fog { Mode off }      

      CGPROGRAM
      #pragma fragmentoption ARB_precision_hint_fastest
      #pragma vertex vertDofApply
      #pragma fragment fragDofApplyBgDebug

      ENDCG
  	}
 
 // pass 3
 
 Pass {
	  ZTest Always Cull Off ZWrite Off
	  ColorMask A
	  Fog { Mode off }      

      CGPROGRAM
      #pragma fragmentoption ARB_precision_hint_fastest
      #pragma vertex vertWithWsReconstruct
      #pragma fragment fragCocBg

      ENDCG
  	}  
  	 	
	
 // pass 4

  
 Pass {
	  ZTest Always Cull Off ZWrite Off
	  Fog { Mode off }      
 	  ColorMask RGB

      CGPROGRAM
      #pragma fragmentoption ARB_precision_hint_fastest
      #pragma vertex vertDofApply
      #pragma fragment fragDofApplyFg
      
      ENDCG
  	}  	

 // pass 5
  
 Pass {
	  ZTest Always Cull Off ZWrite Off
	  Fog { Mode off }      

      CGPROGRAM
      #pragma fragmentoption ARB_precision_hint_fastest
      #pragma vertex vertWithWsReconstruct
      #pragma fragment fragCocAndColorFg

      ENDCG
  	} 

 // pass 6
 
 Pass {
	  ZTest Always Cull Off ZWrite Off
	  Fog { Mode off }      

      CGPROGRAM
      #pragma fragmentoption ARB_precision_hint_fastest
      #pragma vertex vertDownsample
      #pragma fragment fragDownsample

      ENDCG
  	} 

  }
  
Fallback off

}