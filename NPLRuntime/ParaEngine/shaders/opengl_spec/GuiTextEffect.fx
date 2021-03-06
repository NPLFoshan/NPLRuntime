// Author: LiXizhi
// Data: 2006/7
// Desc: for displaying GUI object. 

////////////////////////////////////////////////////////////////////////////////
//  Per frame parameters
float4x4 mWorldViewProj: worldviewprojection;
// texture 0
texture tex0 : TEXTURE; 
sampler tex0Sampler : register(s0) = sampler_state 
{
    texture = <tex0>;
};

struct Interpolants
{
  float4 positionSS			: POSITION;         // Screen space position
  float2 tex				: TEXCOORD0;        // texture coordinates
  float4 color              : TEXCOORD1;
};

////////////////////////////////////////////////////////////////////////////////
//
//                              Vertex Shader
//
////////////////////////////////////////////////////////////////////////////////
Interpolants vertexShader(	float4 Pos : POSITION,
							float2 Tex : TEXCOORD0,
                            float4 Color : TEXCOORD1 )
{
	Interpolants o = (Interpolants)0;
	// screen space position
	o.positionSS = 	mul(Pos, mWorldViewProj);
	o.tex.xy = Tex;
    o.color = Color.rgba;
	return o;
}

////////////////////////////////////////////////////////////////////////////////
//
//                              Pixel Shader
//
////////////////////////////////////////////////////////////////////////////////
float4 pixelShader(Interpolants i) : COLOR
{
	half4 normalColor = tex2D(tex0Sampler, i.tex.xy);
	return float4(i.color.rgb,i.color.a * normalColor.a);
}

////////////////////////////////////////////////////////////////////////////////
//
//                              Technique
//
////////////////////////////////////////////////////////////////////////////////
technique SimpleMesh_vs20_ps20
{
	pass P0
	{
		// shaders
		VertexShader = compile vs_2_0 vertexShader();
		PixelShader  = compile ps_2_0 pixelShader();
		
		FogEnable = false;
	}
}