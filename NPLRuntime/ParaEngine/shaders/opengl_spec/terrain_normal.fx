
float4x4 mWorldViewProj:worldviewprojection;
float4x4 mWorld:world;
float4 sunvector:sunvector;
float4 ambientlight:ambientlight;
float4 texCoordOffset:texCoordOffset;
float3 worldcamerapos:worldcamerapos; 
bool sunlightenable:sunlightenable;
bool k_bBoolean10 : boolean10;
bool fogenable:fogenable;
float4 fogparameters:fogparameters;
float4 materialdiffuse:materialdiffuse;
float4 fogColor:fogColor;

// texture 0: alpha layer
texture tex0 : TEXTURE; 
sampler tex0Sampler : register(s0) = sampler_state 
{
    texture = <tex0>;
	MinFilter = Linear;
	MagFilter = Linear;
	MipFilter = Linear;
	AddressU = Clamp;
	AddressV = Clamp;
};
// texture 1: detail layer
texture tex1 : TEXTURE; 
sampler tex1Sampler : register(s0) = sampler_state 
{
    texture = <tex1>;
	MinFilter = Linear;
	MagFilter = Linear;
	MipFilter = Linear;
	AddressU = wrap;
	AddressV = wrap;
};
// texture 2: base layer
texture tex2 : TEXTURE; 
sampler tex2Sampler : register(s0) = sampler_state 
{
    texture = <tex2>;
	MinFilter = Linear;
	MagFilter = Linear;
	MipFilter = Linear;
	AddressU = Clamp;
	AddressV = Clamp;
};

struct appdata
{
	float4 vertex : POSITION;
	float3 normal : NORMAL;
	float4 uv0	  : TEXCOORD0;
	float4 uv1    : TEXCOORD1;
};

struct v2f
{
  float4 vertex			: POSITION;
  float4 uv0			: TEXCOORD0;
  float4 uv1			: TEXCOORD1;
  float4 colorDiffuse   : COLOR;
};



// Calculates fog factor based upon distance
float CalcFogFactor( float d )
{
    float fogCoeff = 0.0;
	fogCoeff = (d - fogparameters.x) / fogparameters.y;
    return clamp( fogCoeff, 0.0, 1.0);
}

v2f vert(appdata v)
{
	v2f o = (v2f)0;
	o.vertex = mul(v.vertex, mWorldViewProj);
	float3 worldNormal = normalize(v.normal);

	o.colorDiffuse.xyz = materialdiffuse.xyz * dot( sunvector.xyz, worldNormal ) + ambientlight.xyz;

	float3 worldPos = mul(v.vertex,mWorld).xyz;
	float3 eyeVec = normalize(worldcamerapos - worldPos);
	float3 reflectVec = reflect(-sunvector.xyz,worldNormal);
	float specular = max(dot(eyeVec,reflectVec),0.0);
	o.colorDiffuse.w = pow(specular,12.0) * 0.6;

	o.uv0.xy = v.uv0.xy;
	o.uv0.z = CalcFogFactor(length(o.vertex.xyz));
	o.uv1.xy = v.uv1.xy;
	o.uv1.zw = float2((v.vertex.x-texCoordOffset.x)/texCoordOffset.z, (v.vertex.z-texCoordOffset.y)/texCoordOffset.z);

	return o;
}

float4 frag(v2f i) : COLOR
{
	float4 normalColor = float4(0,0,0,1);
	float3 colorDif = i.colorDiffuse.rgb;

		// layer alpha0 * detail1
	float4 color1 = tex2D(tex1Sampler, i.uv1.xy);
	float alpha =  tex2D(tex0Sampler, i.uv0.xy).a;

	normalColor.xyz = color1.xyz * alpha;
	float specularWeight = (1.0 - color1.a) * alpha;

	normalColor.xyz *= colorDif;
	normalColor.xyz += (specularWeight * i.colorDiffuse.www) * materialdiffuse.xyz;

	// multiple base layer
	normalColor.xyz *= tex2D(tex2Sampler,i.uv1.zw).xyz;

	// fog color
	normalColor.xyz *= (1.0-i.uv0.z);
	if(k_bBoolean10)
		normalColor.xyz += fogColor.xyz * i.uv0.z;
	return normalColor;
}

technique default
{
	pass P0
	{
		// shaders
		VertexShader = compile vs_2_0 vert();
		PixelShader  = compile ps_2_0 frag();
	}
}