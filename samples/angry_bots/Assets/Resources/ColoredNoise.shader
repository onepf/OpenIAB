
Shader "Hidden/ColoredNoise" {
	Properties {
		_MainTex ("Base (RGB)", 2D) = "white" {}
		_NoiseTex ("Noise (RGB)", 2D) = "white" {}
	}
	
	CGINCLUDE

		#include "UnityCG.cginc"

		sampler2D _MainTex;
		sampler2D _NoiseTex;
		
		#if SHADER_API_D3D9
			uniform half4 _MainTex_TexelSize;
		#endif

		uniform half _NoiseAmount;
		
		struct v2f {
			float4 pos : SV_POSITION;
			half2 uv : TEXCOORD0;
			#if SHADER_API_D3D9
			half4 uv_screen : TEXCOORD1;	
			#else
			half2 uv_screen : TEXCOORD1;
			#endif		
		};
				
		v2f vert (appdata_img v)
		{
			v2f o;
			
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);	
			o.uv = v.texcoord.xy;
			
			#if SHADER_API_D3D9
			o.uv_screen = v.vertex.xyxy;
			if (_MainTex_TexelSize.y < 0)
        		o.uv_screen.y = 1-o.uv_screen.y;
        	#else
        		o.uv_screen = v.vertex.xy;
			#endif
			
			return o; 
		}

		half4 frag ( v2f i ) : COLOR
		{	
			half4 color = tex2D (_MainTex, i.uv_screen.xy);
			half4 noise = (tex2D (_NoiseTex, i.uv.xy)  ) * _NoiseAmount;			
			
			noise *= (0.5-Luminance(color.rgb));
			
			return color + noise;
		} 
	
	ENDCG
	
	SubShader {
	  ZTest Always Cull Off ZWrite Off Blend Off
	  Fog { Mode off }  
	  
	Pass {
	
		CGPROGRAM
		
		#pragma vertex vert
		#pragma fragment frag
		#pragma fragmentoption ARB_precision_hint_fastest 
		
		ENDCG
		 
		}				
	}
	FallBack Off
}
