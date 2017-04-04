<?php
class wire {
    private $key, $cipher, $data, $mode, $iv;
 
    public function __construct($key=null,$block_size=null,$mode=null){
        $this->set_key($key);
        $this->set_block_size($block_size);
        $this->set_mode($mode);
    }
 
    public function set_data($data){
        $this->data = $data;
    }
 
    public function set_key($key){
        $this->key = $key;
    }
 
    public function set_block_size($block_size){
        if($block_size==192)
            $this->cipher = MCRYPT_RIJNDAEL_192;
        elseif($block_size==256)
            $this->cipher = MCRYPT_RIJNDAEL_256;
        else
            $this->cipher = MCRYPT_RIJNDAEL_128;
    }
 
    public function set_mode($mode){
        if($mode=='cbc')
            $this->mode = MCRYPT_MODE_CBC;
        elseif($mode=='cfb')
            $this->mode = MCRYPT_MODE_CFB;
        elseif($mode=='ebc')
            $this->mode = MCRYPT_MODE_ECB;
        elseif($mode=='nofb')
            $this->mode = MCRYPT_MODE_NOFB;
        elseif($mode=='ofb')
            $this->mode = MCRYPT_MODE_OFB;
        elseif($mode=='stream')
            $this->mode = MCRYPT_MODE_STREAM;
        else
            $this->mode = MCRYPT_MODE_ECB;
    }
 
    public function set_iv($iv){
        $this->iv = ($iv=='')
            ? mcrypt_create_iv(mcrypt_get_iv_size($this->cipher,$this->mode),MCRYPT_RAND)
            : $iv;
    }
 
    public function encrypt($data){
        $encrypt = mcrypt_encrypt($this->cipher,$this->key,$data,$this->mode,$this->iv);
        return trim(base64_encode($encrypt));
    }
 
    public function decrypt($data){
        $decrypt = mcrypt_decrypt($this->cipher,$this->key,base64_decode($data),$this->mode,$this->iv);
        return trim($decrypt);
    }
}
?>