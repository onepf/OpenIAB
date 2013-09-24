
Shader "AngryBots/FX/ScreenRefract" {
	Properties {
		_MainTex ("Base (RGB)", 2D) = "white" {}
		_ShimmerDistort ("Distort (in RG channels)", 2D) = "black" {}
		_Distort ("Distort", Range(0.0,0.25)) = 0.05
	}
	
	CGINCLUDE

		#include "UnityCG.cginc"

		sampler2D _MainTex;
		sampler2D _HeatShimmerTex;
		sampler2D _ShimmerDistort;
		
		uniform float4 _MainTex_ST;
		uniform half _Distort;
		
		struct v2f {
			half4 pos : SV_POSITION;
			half2 uv : TEXCOORD0;	
			half4 uvScreen : TEXCOORD1;	
		};

		v2f vert (appdata_full v)
		{
			v2f o;

			o.uv.xy = TRANSFORM_TEX(v.texcoord.xy, _MainTex);
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);	
			o.uvScreen = ComputeScreenPos (o.pos);
			
			return o; 
		}
		
		half4 frag ( v2f i ) : COLOR
		{	
			fixed4 normal = tex2D (_ShimmerDistort, i.uv.xy)-0.5;
			i.uvScreen.xy += normal.xy * _Distort;
			fixed4 screen = tex2Dproj (_HeatShimmerTex, UNITY_PROJ_COORD (i.uvScreen));
			// screen.a = i.vColor.a;
			screen.a = tex2D (_MainTex, i.uv.xy).a;
			return screen;// * tex2D(_MainTex, i.uv.xy);
		}
	
	ENDCG
	
	SubShader {
		Tags { "Queue"="Transparent+100"  "RenderType"="Transparent" }
		Cull Off
		ZWrite Off
		ZTest LEqual
		Blend SrcAlpha OneMinusSrcAlpha
		
	GrabPass { "_HeatShimmerTex" }
		
	Pass {
	
		CGPROGRAM
		
		#pragma vertex vert
		#pragma fragment frag
		#pragma fragmentoption ARB_precision_hint_fastest 
		
		ENDCG
		 
		}
				
	} 
	FallBack "Transparent/Diffuse"
}
