import React, { Component } from 'react';
import Header from '../../Header';
import Joi from 'joi-browser';
import Form from '../../../components/Form/Form';
import { post } from '../../../utils/api';
import { uriSchemaCreate } from '../../../utils/endpoints';

class SchemaCreate extends Form {
  state = {
    formData: {
      subject: '',
      compatibilityLevel: 'BACKWARD',
      schemaData: ''
    },
    compatibilityLevelSelect: [
      { _id: 'NONE', name: 'NONE' },
      { _id: 'BACKWARD', name: 'BACKWARD' },
      { _id: 'BACKWARD_TRANSITIVE', name: 'BACKWARD_TRANSITIVE' },
      { _id: 'FORWARD', name: 'FORWARD' },
      { _id: 'FORWARD_TRANSITIVE', name: 'FORWARD_TRANSITIVE' },
      { _id: 'FULL', name: 'FULL' },
      { _id: 'FULL_TRANSITIVE', name: 'FULL_TRANSITIVE' }
    ],
    errors: {}
  };

  schema = {
    subject: Joi.string()
      .required()
      .label('subject'),
    compatibilityLevel: Joi.string()
      .label('Compatibility')
      .required(),
    schemaData: Joi.string()
      .label('SchemaData')
      .required()
  };

  onCleanupChange = value => {
    let { formData } = { ...this.state };
    formData.compatibilityLevel = value;

    this.setState({ formData });
  };

  async doSubmit() {
    const { formData } = this.state;
    const { clusterId } = this.props.match.params;
    const { history } = this.props;
    const schema = {
      cluster: clusterId,
      subject: formData.subject,
      schema: formData.schemaData,
      compatibilityLevel: formData.compatibilityLevel
    };
    history.replace({
      loading: true
    });
    post(uriSchemaCreate(clusterId), schema)
      .then(res => {
        this.props.history.push({
          pathname: `/ui/${clusterId}/schema`,
          showSuccessToast: true,
          successToastMessage: `Schema '${formData.subject}' is created`,
          loading: false
        });
      })
      .catch(err => {
        console.log('err', err);
        this.props.history.replace({
          showErrorToast: true,
          errorToastTitle: `Failed to create schema '${formData.subject}'`,
          errorToastMessage: err.response.data.message,
          loading: false
        });
      });
  }

  render() {
    const { compatibilityLevelSelect, formData } = this.state;
    return (
      <div>
        <form
          encType="multipart/form-data"
          className="khq-form khq-form-config"
          onSubmit={() => this.doSubmit()}
        >
          <Header title="Create a schema" history={this.props.history} />
          {this.renderInput('subject', 'Subject', 'Subject')}

          {this.renderSelect(
            'compatibilityLevel',
            'Compatibility Level',
            compatibilityLevelSelect,
            value => {
              this.setState({ formData: { ...formData, compatibilityLevel: value.target.value } });
            }
          )}

          {this.renderJSONInput('schemaData', 'Schema', value => {
            this.setState({
              formData: {
                ...formData,
                schemaData: value
              }
            });
          })}
          {this.renderButton(
            'Create',
            () => {
              this.doSubmit();
            },
            undefined,
            'button'
          )}
        </form>
      </div>
    );
  }
}

export default SchemaCreate;
