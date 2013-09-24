
Shader "AngryBots/FX/HeightFogBeforeTransparent" {
	
Properties {
	_FogColor ("FogColor", COLOR) = (.0,.0,.0,.0)
	_Exponent ("Exponent", Range(0.1, 4.0)) = 0.3
	_Y("Y", Range(-30.0, 30.0)) = 0.0
}

SubShader {	
	LOD 300
	Tags { "RenderType"="Transparent" "Queue" = "Transparent-1" }
	
	Pass {
		ZTest Always Cull Off ZWrite Off
		Blend OneMinusSrcAlpha SrcAlpha
		Fog { Mode off }

		CGPROGRAM

		#pragma vertex vert
		#pragma fragment frag
		#pragma fragmentoption ARB_precision_hint_fastest 
		#include "UnityCG.cginc"
		
		uniform sampler2D _CameraDepthTexture;
				
		uniform float  _Y;

		uniform float _Exponent;
		uniform float4 _FogColor;
		
		uniform float4 _MainTex_TexelSize;
		
		uniform float4x4 _FrustumCornersWS;
		uniform float4 _CameraWS;
		 
		struct v2f {
			float4 pos : POSITION;
			float4 uv : TEXCOORD0;
			float4 interpolatedRay : TEXCOORD1;
		};
		
		v2f vert( appdata_full v )
		{
			v2f o;
			int index = (int)v.texcoord.y;

			o.pos = mul(UNITY_MATRIX_MVP, v.vertex);
			o.uv = ComputeScreenPos(o.pos);			
			
			o.interpolatedRay = _FrustumCornersWS[index];
			o.interpolatedRay.w = index;
			return o;
		}

		
		half4 frag (v2f i) : COLOR
		{
			float dpth = UNITY_SAMPLE_DEPTH (tex2Dproj (_CameraDepthTexture, UNITY_PROJ_COORD(i.uv)));
			dpth = Linear01Depth (dpth);
			float4 wsDir = (_CameraWS + dpth * i.interpolatedRay);

			float fogIntensity = (_Y - wsDir.y);
			fogIntensity = saturate (exp (-fogIntensity * _Exponent));
			
			half4 outColor = _FogColor;
			outColor.a = fogIntensity;
					
			return outColor;
		}
		
		ENDCG
	}
}

Fallback off
}