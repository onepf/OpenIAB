
Shader "AngryBots/FX/Rain" {
	Properties {
		_MainTex ("Base (RGB)", 2D) = "white" {}
	}
	
	CGINCLUDE

		#include "UnityCG.cginc"

		sampler2D _MainTex;
		
		uniform float4 _MainTex_ST;
				
		struct v2f {
			half4 pos : SV_POSITION;
			half2 uv : TEXCOORD0;		
		};

		v2f vert(appdata_full v)
		{
			v2f o;
			
			v.vertex.yzx += v.texcoord1.xyy;
			o.uv.xy = TRANSFORM_TEX(v.texcoord.xy, _MainTex);
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);	
						
			return o; 
		}
		
		fixed4 frag( v2f i ) : COLOR
		{	
			return tex2D(_MainTex, i.uv);
		}
	
	ENDCG
	
	SubShader {
		Tags { "Queue"="Transparent"  "Queue" = "Transparent" }
		Cull Off
		ZWrite Off
		Blend SrcAlpha One
		
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
