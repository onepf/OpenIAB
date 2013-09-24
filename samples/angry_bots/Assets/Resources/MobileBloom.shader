
Shader "Hidden/MobileBloom" {
	Properties {
		_MainTex ("Base (RGB)", 2D) = "white" {}
		_Bloom ("Bloom (RGB)", 2D) = "black" {}
	}
	
	CGINCLUDE

		#include "UnityCG.cginc"

		sampler2D _MainTex;
		sampler2D _Bloom;
		
		uniform fixed4 _ColorMix;	
		
		uniform half4 _MainTex_TexelSize;
		uniform fixed4 _Parameter;
		
		#define ONE_MINUS_INTENSITY _Parameter.w

		struct v2f_simple {
			half4 pos : SV_POSITION;
			half4 uv : TEXCOORD0;
		};
		
		struct v2f_withMaxCoords {
			half4 pos : SV_POSITION;
			half2 uv : TEXCOORD0;
			half2 uv2[4] : TEXCOORD1;
		};		

		struct v2f_withBlurCoords {
			half4 pos : SV_POSITION;
			half2 uv2[4] : TEXCOORD0;
		};	
		
		v2f_simple vertBloom (appdata_img v)
		{
			v2f_simple o;
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
        	o.uv = v.texcoord.xyxy;			
        	#if SHADER_API_D3D9
        		if (_MainTex_TexelSize.y < 0.0)
        			o.uv.w = 1.0 - o.uv.w;
        	#endif
			return o; 
		}

		v2f_withMaxCoords vertMax (appdata_img v)
		{
			v2f_withMaxCoords o;
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
        	o.uv = v.texcoord;
        	o.uv2[0] = v.texcoord + _MainTex_TexelSize.xy * half2(1.5,1.5);					
			o.uv2[1] = v.texcoord + _MainTex_TexelSize.xy * half2(-1.5,1.5);
			o.uv2[2] = v.texcoord + _MainTex_TexelSize.xy * half2(-1.5,-1.5);
			o.uv2[3] = v.texcoord + _MainTex_TexelSize.xy * half2(1.5,-1.5);
			return o; 
		}			

		v2f_withBlurCoords vertBlurVertical (appdata_img v)
		{
			v2f_withBlurCoords o;
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
        	o.uv2[0] = v.texcoord + _MainTex_TexelSize.xy * half2(0.0, -1.5);			
			o.uv2[1] = v.texcoord + _MainTex_TexelSize.xy * half2(0.0, -0.5);	
			o.uv2[2] = v.texcoord + _MainTex_TexelSize.xy * half2(0.0, 0.5);	
			o.uv2[3] = v.texcoord + _MainTex_TexelSize.xy * half2(0.0, 1.5);	
			return o; 
		}	

		v2f_withBlurCoords vertBlurHorizontal (appdata_img v)
		{
			v2f_withBlurCoords o;
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
        	o.uv2[0] = v.texcoord + _MainTex_TexelSize.xy * half2(-1.5, 0.0);			
			o.uv2[1] = v.texcoord + _MainTex_TexelSize.xy * half2(-0.5, 0.0);	
			o.uv2[2] = v.texcoord + _MainTex_TexelSize.xy * half2(0.5, 0.0);	
			o.uv2[3] = v.texcoord + _MainTex_TexelSize.xy * half2(1.5, 0.0);	
			return o; 
		}	
						
		fixed4 fragBloom ( v2f_simple i ) : COLOR
		{	
			fixed4 color = tex2D(_MainTex, i.uv.xy);
			return color + tex2D(_Bloom, i.uv.zw);
		} 
		
		fixed4 fragBloomWithColorMix ( v2f_simple i ) : COLOR
		{	
			fixed4 color = tex2D(_MainTex, i.uv.xy);	
					
			half colorDistance = Luminance(abs(color.rgb-_ColorMix.rgb));
			color = lerp(color, _ColorMix, (_Parameter.x*colorDistance));
			color += tex2D(_Bloom, i.uv.zw);			
						
			return color;					
		} 
		
		fixed4 fragMaxWithPain ( v2f_withMaxCoords i ) : COLOR
		{				
			fixed4 color = tex2D(_MainTex, i.uv.xy);
			color = max(color, tex2D (_MainTex, i.uv2[0]));	
			color = max(color, tex2D (_MainTex, i.uv2[1]));	
			color = max(color, tex2D (_MainTex, i.uv2[2]));	
			color = max(color, tex2D (_MainTex, i.uv2[3]));	
			return saturate(color + half4(0.25,0,0,0) - ONE_MINUS_INTENSITY);
		} 
		
		fixed4 fragMax ( v2f_withMaxCoords i ) : COLOR
		{				
			fixed4 color = tex2D(_MainTex, i.uv.xy);
			color = max(color, tex2D (_MainTex, i.uv2[0]));	
			color = max(color, tex2D (_MainTex, i.uv2[1]));	
			color = max(color, tex2D (_MainTex, i.uv2[2]));	
			color = max(color, tex2D (_MainTex, i.uv2[3]));	
			return saturate(color - ONE_MINUS_INTENSITY);
		} 

		fixed4 fragBlurForFlares ( v2f_withBlurCoords i ) : COLOR
		{				
			fixed4 color = tex2D (_MainTex, i.uv2[0]);
			color += tex2D (_MainTex, i.uv2[1]);
			color += tex2D (_MainTex, i.uv2[2]);
			color += tex2D (_MainTex, i.uv2[3]);
			return color * 0.25;
		}
			
	ENDCG
	
	SubShader {
	  ZTest Always Cull Off ZWrite Off Blend Off
	  Fog { Mode off }  
	  
	// 0
	Pass {
		CGPROGRAM
		
		#pragma vertex vertBloom
		#pragma fragment fragBloom
		#pragma fragmentoption ARB_precision_hint_fastest 
		
		ENDCG
		}
	// 1
	Pass { 
		CGPROGRAM
		
		#pragma vertex vertMax
		#pragma fragment fragMax
		#pragma fragmentoption ARB_precision_hint_fastest 
		
		ENDCG	 
		}	
	// 2
	Pass {
		CGPROGRAM
		
		#pragma vertex vertBlurVertical
		#pragma fragment fragBlurForFlares
		#pragma fragmentoption ARB_precision_hint_fastest 
		
		ENDCG 
		}	
	// 3			
	Pass {
		CGPROGRAM
		
		#pragma vertex vertBlurHorizontal
		#pragma fragment fragBlurForFlares
		#pragma fragmentoption ARB_precision_hint_fastest 
		
		ENDCG
		}
	// 4			
	Pass {
		CGPROGRAM
		
		#pragma vertex vertBloom
		#pragma fragment fragBloomWithColorMix
		#pragma fragmentoption ARB_precision_hint_fastest 
		
		ENDCG
		}
	// 5			
	Pass {
		CGPROGRAM
		
		#pragma vertex vertMax
		#pragma fragment fragMaxWithPain
		#pragma fragmentoption ARB_precision_hint_fastest 
		
		ENDCG
		}
	}
	FallBack Off
}
