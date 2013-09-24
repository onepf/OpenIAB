/* 

replacement shaders for ...

cheaper/better rendering of reflections for low end / mobile platforms. we will 
only render the types tagged here into the reflection buffer, thus freeing up
many CPU and GPU cycles.

*/

Shader "Hidden/RealtimeReflectionReplacement" {
	
Properties {
	_MainTex ("Base (RGB)", 2D) = "white" {}
	_Normal("Normal", 2D) = "bump" {}
	_TintColor ("TintColor", Color) = (1,1,1,1)
}

CGINCLUDE		

struct v2f 
{
	half4 pos : SV_POSITION;
	half2 uv : TEXCOORD0;
};

struct v2f_full
{
	half4 pos : SV_POSITION;
	half4 uv : TEXCOORD0;
	half3 tsBase0 : TEXCOORD2;
	half3 tsBase1 : TEXCOORD3;
	half3 tsBase2 : TEXCOORD4;	
	half3 viewDirNotNormalized : TEXCOORD5;
};
	
#include "UnityCG.cginc"		

sampler2D _MainTex;
sampler2D _Normal;		
						
ENDCG 


SubShader {
	Tags { "Reflection" = "RenderReflectionOpaque" }
	LOD 200 
    Fog { Mode Off }
	
	Pass {
		
		CGPROGRAM

		half4 _MainTex_ST;		
				
		v2f vert (appdata_full v) 
		{
			v2f o;
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
			o.uv = TRANSFORM_TEX(v.texcoord, _MainTex);
			return o; 
		}		
		
		fixed4 frag (v2f i) : COLOR0 
		{
			fixed4 tex = tex2D (_MainTex, i.uv);
			tex.rgb = tex.rgb * 0.5 + 2.5 * tex.rgb * tex.a;
			return tex;		
		}	
		
		#pragma vertex vert
		#pragma fragment frag
		#pragma fragmentoption ARB_precision_hint_fastest 
	
		ENDCG
	}
} 

SubShader {
	Tags { "Reflection" = "RenderReflectionTransparentAdd" }
	
	LOD 200 
    Fog { Mode Off }
	Blend One One
	Cull Off
	ZWrite Off
	ZTest Always
	
	Pass {
        Cull Off
		
		CGPROGRAM

		half4 _MainTex_ST;		
		fixed4 _TintColor;
				
		v2f vert (appdata_full v) 
		{
			v2f o;
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
			o.uv = TRANSFORM_TEX(v.texcoord, _MainTex);
			return o; 
		}		
		
		fixed4 frag (v2f i) : COLOR0 
		{
			fixed4 tex = tex2D (_MainTex, i.uv) * 2.0 * _TintColor;
			return tex;		
		}	
		
		#pragma vertex vert
		#pragma fragment frag
		#pragma fragmentoption ARB_precision_hint_fastest 
	
		ENDCG
	}
} 

SubShader {
	Tags { "Reflection" = "RenderReflectionTransparentBlend" }
	
	LOD 200 
    Fog { Mode Off }
	Blend SrcAlpha One
	Cull Off
	ZWrite Off
	
	Pass {
        Cull Off
		
		CGPROGRAM

		half4 _MainTex_ST;		
				
		v2f vert (appdata_full v) 
		{
			v2f o;
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
			o.uv = v.texcoord;
			return o; 
		}		
		
		fixed4 frag (v2f i) : COLOR0 
		{
			fixed4 tex = tex2D (_MainTex, i.uv) * 2.0;
			return tex;		
		}	
		
		#pragma vertex vert
		#pragma fragment frag
		#pragma fragmentoption ARB_precision_hint_fastest 
	
		ENDCG
	}
} 

SubShader {
	Tags { "Reflection" = "LaserScope" }
	
	LOD 200 
    Fog { Mode Off }
	Blend SrcAlpha One
	Cull Off
	ZWrite Off
	
	Pass {
		
		CGPROGRAM

		half4 _MainTex_ST;		
				
		v2f vert (appdata_full v) 
		{
			v2f o;
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
			o.uv = TRANSFORM_TEX(v.texcoord, _MainTex);
			return o; 
		}		
		
		fixed4 frag (v2f i) : COLOR0 
		{
			fixed4 tex = tex2D (_MainTex, i.uv) * 2.0;
			return tex;		
		}	
		
		#pragma vertex vert
		#pragma fragment frag
		#pragma fragmentoption ARB_precision_hint_fastest 
	
		ENDCG
	}
} 

FallBack Off
}

