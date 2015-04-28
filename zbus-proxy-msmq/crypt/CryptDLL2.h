#ifndef CRYPTDLL_H
#define CRYPTDLL_H




/*******************************************************************************************************************
解码接口,
返回解码后的字符串长度, 出错则返回-1
*/
int DecryptStr(char * PublicKey, char * PrivateKey, char * EnStr,  char * ClearStr, int ClearStrBufSize);


/*******************************************************************************************************************
解码接口,
返回解码后的字符串长度, 出错则返回-1
需要的key从ini文件中加载,格式为:

--sample--

[KEY]
Index = 1
PublicKey =BD9D3BE806B106D1D95A70FFEA6836FF7A8D4111951381B6E7C683D5302F0523D214FD3B7FB5D806608158F24496B76B339E60DF3591CC5337CE437575F2EAA9
PrivateKey= 19EA2C6ED8C3471855297C3A2DD25279F648E9F4BA3831494B42217078A664803580DD6686401973775A4432F8E7794A0EFC4333D5822285B29D802578458305

Index = 2
PublicKey =A1D8810AAC4B26AFE1782CE58B4C28AECBD0ACC2C3E464CC33B815AA14310D861224C0F974632772C7168F1E54AF1CD34E860FCB042F91481B144ED66572D7AD
PrivateKey= 88E74838CC078201331B61F705FEF546EEF764BC17D282054E168094EF21934AA63F491DF16099588A2C53088DFCE6890526C1BCF3275AFC73ACF77F49B9A541

Index = 3
PublicKey =D6AEB07C340C12F318625676985E5280748004499E23AD9AFB6B647EA7AEF6E1D4293F9E6D265B7EC1F49E4424960C57167415FE1E69C9E22E023A80E9219F8B
PrivateKey= C1ABF8B523F31C89481F36E7688FF717C181D7DC5E18CBBE3C40E6F947E714FA80DB3AAD613486969F28CCC64A8DA7813537D875254C30FF70F03B1E6D5775F1


--
*/
int DecryptStrWithIni(char * EnStr,  char * ClearStr, int ClearStrBufSize);



/*******************************************************************************************************************
//对于大数据量的加密, RSA会非常耗费CPU, 对此进行专门进行优化
*/
int DecryptStrLarge(char * PublicKey, char * PrivateKey, char * EnStr,  char * ClearStr, int ClearStrBufSize);
int DecryptStrWithIniLarge(char * EnStr,  char * ClearStr, int ClearStrBufSize);


#endif //CRYPTDLL_H