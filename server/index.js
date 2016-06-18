var AWS = require('aws-sdk')
var fs = require('fs')
var s3 = new AWS.S3()
var express = require('express')
var app = express()
var bucketName = 'media-bruno'
var bodyParser = require('body-parser')
var multer  = require('multer')

var upload = multer({
  storage: multer.diskStorage({
    destination: './uploads'
  }),
  limits: {
    fileSize: 10 * 1024 * 1024 //10Mb
  }
});

app.use(bodyParser.urlencoded({extended: true}))
app.use(bodyParser.json())

app.post('/upload', upload.single('file'), function(req, res){
  if (!req.file) {
    return res.status(400).json({
      message: 'A file is required.'
    });
  }

  var filename = req.headers['x-file-name'] || 'custom_name_'+new Date()+'.jpg'
  var filepath = req.file.path
  var mimeType = req.headers['x-file-mime-type']
  uploadS3(filepath, filename, mimeType, function(err, result){
    if(err){
      console.log(err);
      res.status(400).send(err)
    }else{
      res.status(200).send(result)
      fs.unlink(filepath, (err)=>{
        if(err) return console.log('Error while deleting file ' + filepath);
        console.log('Successfully deleted file ' + filepath);
      })
    }
  })
})

app.listen(3000, function () {
  console.log('Example app listening on port 3000!');
});

function uploadS3(path, filename, mimeType, cb){
  var body = fs.readFileSync(path)
  var s3bucket = new AWS.S3({params: {Bucket: bucketName}})
  var params = {
    ACL: 'public-read',
    Key: 'upload-service/'+filename,
    Body: body,
    ContentType: mimeType
  }
  s3bucket.upload(params, cb)
}
